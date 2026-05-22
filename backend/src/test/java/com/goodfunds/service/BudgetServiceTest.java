package com.goodfunds.service;

import com.goodfunds.domain.Budget;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.BudgetRequest;
import com.goodfunds.dto.BudgetResponse;
import com.goodfunds.exception.BudgetNotFoundException;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.exception.DuplicateBudgetException;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void list_withRef_returnsFilteredBudgets() {
        User user = buildUser();
        Category cat = buildCategory(user);
        Budget budget = buildBudget(user, cat, new BigDecimal("500.00"), 5, 2026);
        YearMonth ref = YearMonth.of(2026, 5);

        when(budgetRepository.findByUserIdAndAnoAndMes(user.getId(), 2026, 5))
                .thenReturn(List.of(budget));

        List<BudgetResponse> result = budgetService.list(user.getId(), ref);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mes()).isEqualTo(5);
        assertThat(result.get(0).ano()).isEqualTo(2026);
        assertThat(result.get(0).limite()).isEqualByComparingTo("500.00");
    }

    @Test
    void list_withoutRef_returnsAllUserBudgets() {
        User user = buildUser();
        Category cat = buildCategory(user);
        Budget b1 = buildBudget(user, cat, new BigDecimal("300.00"), 4, 2026);
        Budget b2 = buildBudget(user, cat, new BigDecimal("500.00"), 5, 2026);

        when(budgetRepository.findByUserId(user.getId())).thenReturn(List.of(b1, b2));

        List<BudgetResponse> result = budgetService.list(user.getId(), null);

        assertThat(result).hasSize(2);
        verify(budgetRepository, never()).findByUserIdAndAnoAndMes(any(), any(), any());
    }

    @Test
    void create_persistsBudgetAndReturnsResponse() {
        User user = buildUser();
        Category cat = buildCategory(user);
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), cat.getId(), 5, 2026);

        when(categoryRepository.findByIdAndUserId(cat.getId(), user.getId())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), cat.getId(), 2026, 5))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(budgetRepository.saveAndFlush(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        BudgetResponse response = budgetService.create(user.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.limite()).isEqualByComparingTo("500.00");
        assertThat(response.mes()).isEqualTo(5);
        assertThat(response.ano()).isEqualTo(2026);
        assertThat(response.category().id()).isEqualTo(cat.getId());

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getCategory()).isSameAs(cat);
    }

    @Test
    void create_whenDuplicateExists_throwsDuplicateBudget() {
        User user = buildUser();
        Category cat = buildCategory(user);
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), cat.getId(), 5, 2026);
        Budget existing = buildBudget(user, cat, new BigDecimal("300.00"), 5, 2026);

        when(categoryRepository.findByIdAndUserId(cat.getId(), user.getId())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), cat.getId(), 2026, 5))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.create(user.getId(), request))
                .isInstanceOf(DuplicateBudgetException.class)
                .hasMessageContaining(cat.getId().toString());

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_whenCategoryNotOwnedByUser_throwsCategoryNotFound() {
        User user = buildUser();
        UUID unknownCategoryId = UUID.randomUUID();
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), unknownCategoryId, 5, 2026);

        when(categoryRepository.findByIdAndUserId(unknownCategoryId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.create(user.getId(), request))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_modifiesFieldsAndReturnsResponse() {
        User user = buildUser();
        Category cat = buildCategory(user);
        Budget budget = buildBudget(user, cat, new BigDecimal("300.00"), 5, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("800.00"), cat.getId(), 6, 2026);

        when(budgetRepository.findByIdAndUserId(budget.getId(), user.getId())).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(cat.getId(), user.getId())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), cat.getId(), 2026, 6))
                .thenReturn(Optional.empty());
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);

        BudgetResponse response = budgetService.update(user.getId(), budget.getId(), request);

        assertThat(response.limite()).isEqualByComparingTo("800.00");
        assertThat(response.mes()).isEqualTo(6);
        assertThat(response.ano()).isEqualTo(2026);
    }

    @Test
    void update_whenBudgetNotFound_throwsBudgetNotFound() {
        User user = buildUser();
        Category cat = buildCategory(user);
        UUID unknownId = UUID.randomUUID();
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), cat.getId(), 5, 2026);

        when(budgetRepository.findByIdAndUserId(unknownId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(user.getId(), unknownId, request))
                .isInstanceOf(BudgetNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_whenConflictWithAnotherBudget_throwsDuplicateBudget() {
        User user = buildUser();
        Category cat = buildCategory(user);
        Budget budget = buildBudget(user, cat, new BigDecimal("300.00"), 5, 2026);
        Budget otherBudget = buildBudget(user, cat, new BigDecimal("400.00"), 6, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), cat.getId(), 6, 2026);

        when(budgetRepository.findByIdAndUserId(budget.getId(), user.getId())).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(cat.getId(), user.getId())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), cat.getId(), 2026, 6))
                .thenReturn(Optional.of(otherBudget));

        assertThatThrownBy(() -> budgetService.update(user.getId(), budget.getId(), request))
                .isInstanceOf(DuplicateBudgetException.class);

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_sameComboAsItself_doesNotThrowDuplicate() {
        User user = buildUser();
        Category cat = buildCategory(user);
        Budget budget = buildBudget(user, cat, new BigDecimal("300.00"), 5, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("600.00"), cat.getId(), 5, 2026);

        when(budgetRepository.findByIdAndUserId(budget.getId(), user.getId())).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(cat.getId(), user.getId())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), cat.getId(), 2026, 5))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);

        BudgetResponse response = budgetService.update(user.getId(), budget.getId(), request);

        assertThat(response.limite()).isEqualByComparingTo("600.00");
        verify(budgetRepository).saveAndFlush(budget);
    }

    private static User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .nome("Test")
                .email("test@example.com")
                .senha("hash")
                .build();
    }

    private static Category buildCategory(User user) {
        return Category.builder()
                .id(UUID.randomUUID())
                .nome("Alimentacao")
                .tipo(TipoCategoria.DESPESA)
                .user(user)
                .build();
    }

    private static Budget buildBudget(User user, Category category, BigDecimal limite, int mes, int ano) {
        return Budget.builder()
                .id(UUID.randomUUID())
                .limite(limite)
                .category(category)
                .mes(mes)
                .ano(ano)
                .user(user)
                .build();
    }
}
