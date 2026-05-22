package com.goodfunds.service;

import com.goodfunds.domain.Budget;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.User;
import com.goodfunds.dto.BudgetRequest;
import com.goodfunds.dto.BudgetResponse;
import com.goodfunds.exception.BudgetNotFoundException;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.exception.DuplicateBudgetException;
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
        List<Budget> budgets = (ref == null)
                ? budgetRepository.findByUserId(userId)
                : budgetRepository.findByUserIdAndAnoAndMes(userId, ref.getYear(), ref.getMonthValue());
        return budgets.stream().map(BudgetResponse::from).toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        Category category = loadCategoryForUser(request.categoryId(), userId);

        if (budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(
                userId, category.getId(), request.ano(), request.mes()).isPresent()) {
            throw new DuplicateBudgetException(category.getId(), request.mes(), request.ano());
        }

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

        budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(
                        userId, category.getId(), request.ano(), request.mes())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateBudgetException(category.getId(), request.mes(), request.ano());
                });

        budget.setLimite(request.limite());
        budget.setCategory(category);
        budget.setMes(request.mes());
        budget.setAno(request.ano());

        Budget saved = budgetRepository.saveAndFlush(budget);
        return BudgetResponse.from(saved);
    }

    private Category loadCategoryForUser(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
