package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.dto.TransactionCategoryRequest;
import com.goodfunds.dto.TransactionRequest;
import com.goodfunds.dto.TransactionResponse;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.exception.InvalidTransactionFilterException;
import com.goodfunds.exception.TransactionNotFoundException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionService {

    static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "data", "valor", "descricao", "createdAt", "updatedAt", "formaPagamento");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportCacheService reportCacheService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository,
                              ReportCacheService reportCacheService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.reportCacheService = reportCacheService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(UUID userId,
                                            YearMonth ref,
                                            UUID categoryId,
                                            TipoCategoria tipo,
                                            LocalDate from,
                                            LocalDate to,
                                            Pageable pageable) {
        validateDateFilters(ref, from, to);
        Pageable safePageable = sanitizePageable(pageable);

        Specification<Transaction> spec = Specification.allOf(
                TransactionSpecifications.ownedBy(userId),
                TransactionSpecifications.inMonth(ref),
                TransactionSpecifications.hasCategory(categoryId),
                TransactionSpecifications.hasCategoryTipo(tipo),
                TransactionSpecifications.dateFrom(from),
                TransactionSpecifications.dateTo(to));

        return transactionRepository.findAll(spec, safePageable).map(TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        // getReferenceById devolve um proxy sem query; basta para popular o FK.
        User userRef = userRepository.getReferenceById(userId);
        Category category = loadCategoryForUser(request.categoryId(), userId);

        Transaction transaction = Transaction.builder()
                .descricao(request.descricao().trim())
                .valor(request.valor())
                .data(request.data())
                .formaPagamento(request.formaPagamento())
                .category(category)
                .user(userRef)
                .build();

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        reportCacheService.evictUser(userId);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        Category category = loadCategoryForUser(request.categoryId(), userId);

        transaction.setDescricao(request.descricao().trim());
        transaction.setValor(request.valor());
        transaction.setData(request.data());
        transaction.setFormaPagamento(request.formaPagamento());
        transaction.setCategory(category);

        // saveAndFlush garante que @UpdateTimestamp seja aplicado antes de mapear a resposta,
        // evitando devolver um updatedAt obsoleto ao cliente.
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        reportCacheService.evictUser(userId);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse updateCategory(UUID userId, UUID id, TransactionCategoryRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        Category category = loadCategoryForUser(request.categoryId(), userId);
        transaction.setCategory(category);

        // saveAndFlush garante que @UpdateTimestamp seja aplicado antes de mapear a resposta,
        // refletindo o updatedAt renovado da recategorizacao.
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        reportCacheService.evictUser(userId);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        transactionRepository.delete(transaction);
        reportCacheService.evictUser(userId);
    }

    private void validateDateFilters(YearMonth ref, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidTransactionFilterException(
                    "Parametro 'from' nao pode ser posterior a 'to'");
        }
        if (ref != null && (from != null || to != null)) {
            throw new InvalidTransactionFilterException(
                    "Parametro 'ref' nao pode ser combinado com 'from'/'to'");
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        Sort sanitized = Sort.by(pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList());
        if (sanitized.isUnsorted()) {
            sanitized = Sort.by(Sort.Direction.DESC, "data");
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitized);
    }

    private Category loadCategoryForUser(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
