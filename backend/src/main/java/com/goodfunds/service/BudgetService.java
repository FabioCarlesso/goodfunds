package com.goodfunds.service;

import com.goodfunds.domain.Budget;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.User;
import com.goodfunds.dto.BudgetRequest;
import com.goodfunds.dto.BudgetResponse;
import com.goodfunds.exception.BudgetAlreadyExistsException;
import com.goodfunds.exception.BudgetNotFoundException;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId, YearMonth ref) {
        return budgetRepository
                .findByUserIdAndAnoAndMesOrderByCategoryNomeAsc(userId, ref.getYear(), ref.getMonthValue())
                .stream()
                .map(BudgetResponse::from)
                .toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        Category category = loadCategoryForUser(request.categoryId(), userId);
        ensureNoConflict(userId, request.categoryId(), request.ano(), request.mes(), null);

        User userRef = userRepository.getReferenceById(userId);
        Budget budget = Budget.builder()
                .limite(request.limite())
                .category(category)
                .mes(request.mes())
                .ano(request.ano())
                .user(userRef)
                .build();

        Budget saved = budgetRepository.saveAndFlush(budget);
        return BudgetResponse.from(saved);
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID id, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        Category category = loadCategoryForUser(request.categoryId(), userId);
        ensureNoConflict(userId, request.categoryId(), request.ano(), request.mes(), id);

        budget.setLimite(request.limite());
        budget.setCategory(category);
        budget.setMes(request.mes());
        budget.setAno(request.ano());

        // saveAndFlush garante que @UpdateTimestamp seja aplicado antes de mapear a resposta,
        // evitando devolver um updatedAt obsoleto ao cliente.
        Budget saved = budgetRepository.saveAndFlush(budget);
        return BudgetResponse.from(saved);
    }

    private void ensureNoConflict(UUID userId, UUID categoryId, Integer ano, Integer mes, UUID currentId) {
        budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(userId, categoryId, ano, mes)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BudgetAlreadyExistsException(categoryId, mes, ano);
                });
    }

    private Category loadCategoryForUser(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
