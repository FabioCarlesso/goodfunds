package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.dto.TransactionRequest;
import com.goodfunds.dto.TransactionResponse;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.exception.TransactionNotFoundException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(User user,
                                            YearMonth ref,
                                            UUID categoryId,
                                            TipoCategoria tipo,
                                            LocalDate from,
                                            LocalDate to,
                                            Pageable pageable) {
        Specification<Transaction> spec = Specification.where(TransactionSpecifications.ownedBy(user.getId()))
                .and(TransactionSpecifications.inMonth(ref))
                .and(TransactionSpecifications.hasCategory(categoryId))
                .and(TransactionSpecifications.hasCategoryTipo(tipo))
                .and(TransactionSpecifications.dateFrom(from))
                .and(TransactionSpecifications.dateTo(to));

        return transactionRepository.findAll(spec, pageable).map(TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse create(User user, TransactionRequest request) {
        Category category = loadCategoryForUser(request.categoryId(), user);

        Transaction transaction = Transaction.builder()
                .descricao(request.descricao().trim())
                .valor(request.valor())
                .data(request.data())
                .formaPagamento(request.formaPagamento())
                .category(category)
                .user(user)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(User user, UUID id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TransactionNotFoundException(id));

        Category category = loadCategoryForUser(request.categoryId(), user);

        transaction.setDescricao(request.descricao().trim());
        transaction.setValor(request.valor());
        transaction.setData(request.data());
        transaction.setFormaPagamento(request.formaPagamento());
        transaction.setCategory(category);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(User user, UUID id) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TransactionNotFoundException(id));
        transactionRepository.delete(transaction);
    }

    private Category loadCategoryForUser(UUID categoryId, User user) {
        return categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
