package com.goodfunds.service;

import com.goodfunds.domain.Budget;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.BudgetRequest;
import com.goodfunds.dto.BudgetResponse;
import com.goodfunds.exception.BudgetAlreadyExistsException;
import com.goodfunds.exception.BudgetNotFoundException;
import com.goodfunds.exception.CategoryNotFoundException;
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
    void list_returnsMonthBudgetsForUser() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao");
        Budget budget = buildBudget(user, category, new BigDecimal("500.00"), 5, 2026);
        when(budgetRepository.findByUserIdAndAnoAndMesOrderByCategoryNomeAsc(user.getId(), 2026, 5))
                .thenReturn(List.of(budget));

        List<BudgetResponse> result = budgetService.list(user.getId(), YearMonth.of(2026, 5));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).limite()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).categoryNome()).isEqualTo("Alimentacao");
        assertThat(result.get(0).mes()).isEqualTo(5);
        assertThat(result.get(0).ano()).isEqualTo(2026);
    }

    @Test
    void create_persistsBudgetAndReturnsResponse() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao");
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), category.getId(), 5, 2026);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), category.getId(), 2026, 5))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(budgetRepository.saveAndFlush(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        BudgetResponse response = budgetService.create(user.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.limite()).isEqualByComparingTo("500.00");
        assertThat(response.categoryId()).isEqualTo(category.getId());

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getCategory()).isSameAs(category);
    }

    @Test
    void create_whenCategoryNotOwned_throwsCategoryNotFound() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();
        BudgetRequest request = new BudgetRequest(new BigDecimal("100.00"), categoryId, 5, 2026);

        when(categoryRepository.findByIdAndUserId(categoryId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.create(user.getId(), request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_whenDuplicateForPeriod_throwsBudgetAlreadyExists() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao");
        Budget existing = buildBudget(user, category, new BigDecimal("300.00"), 5, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("500.00"), category.getId(), 5, 2026);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), category.getId(), 2026, 5))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.create(user.getId(), request))
                .isInstanceOf(BudgetAlreadyExistsException.class);

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_modifiesFieldsAndReturnsResponse() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao");
        Budget budget = buildBudget(user, category, new BigDecimal("300.00"), 5, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("800.00"), category.getId(), 5, 2026);

        when(budgetRepository.findByIdAndUserId(budget.getId(), user.getId()))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), category.getId(), 2026, 5))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);

        BudgetResponse response = budgetService.update(user.getId(), budget.getId(), request);

        assertThat(response.limite()).isEqualByComparingTo("800.00");
        assertThat(budget.getLimite()).isEqualByComparingTo("800.00");
    }

    @Test
    void update_whenBudgetNotFound_throws() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        BudgetRequest request = new BudgetRequest(new BigDecimal("100.00"), UUID.randomUUID(), 5, 2026);

        when(budgetRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(user.getId(), id, request))
                .isInstanceOf(BudgetNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_whenMovingOntoAnotherExistingBudget_throwsBudgetAlreadyExists() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao");
        Budget budget = buildBudget(user, category, new BigDecimal("300.00"), 5, 2026);
        Budget conflicting = buildBudget(user, category, new BigDecimal("700.00"), 6, 2026);
        BudgetRequest request = new BudgetRequest(new BigDecimal("400.00"), category.getId(), 6, 2026);

        when(budgetRepository.findByIdAndUserId(budget.getId(), user.getId()))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndAnoAndMes(user.getId(), category.getId(), 2026, 6))
                .thenReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> budgetService.update(user.getId(), budget.getId(), request))
                .isInstanceOf(BudgetAlreadyExistsException.class);

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    private static User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .nome("Test")
                .email("test@example.com")
                .senha("hash")
                .build();
    }

    private static Category buildCategory(User user, String nome) {
        return Category.builder()
                .id(UUID.randomUUID())
                .nome(nome)
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
