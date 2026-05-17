package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.CategoryRequest;
import com.goodfunds.dto.CategoryResponse;
import com.goodfunds.exception.CategoryInUseException;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void list_withoutTipo_returnsAllUserCategoriesOrderedByName() {
        User user = buildUser();
        Category alimentacao = buildCategory(user, "Alimentacao", TipoCategoria.DESPESA);
        Category salario = buildCategory(user, "Salario", TipoCategoria.RECEITA);
        when(categoryRepository.findByUserIdOrderByNomeAsc(user.getId()))
                .thenReturn(List.of(alimentacao, salario));

        List<CategoryResponse> result = categoryService.list(user.getId(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nome()).isEqualTo("Alimentacao");
        assertThat(result.get(1).nome()).isEqualTo("Salario");
        verify(categoryRepository, never()).findByUserIdAndTipoOrderByNomeAsc(any(), any());
    }

    @Test
    void list_withTipo_filtersByTipo() {
        User user = buildUser();
        Category salario = buildCategory(user, "Salario", TipoCategoria.RECEITA);
        when(categoryRepository.findByUserIdAndTipoOrderByNomeAsc(user.getId(), TipoCategoria.RECEITA))
                .thenReturn(List.of(salario));

        List<CategoryResponse> result = categoryService.list(user.getId(), TipoCategoria.RECEITA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tipo()).isEqualTo(TipoCategoria.RECEITA);
        verify(categoryRepository, never()).findByUserIdOrderByNomeAsc(any());
    }

    @Test
    void create_persistsCategoryAndReturnsResponse() {
        User user = buildUser();
        CategoryRequest request = new CategoryRequest("Investimentos", TipoCategoria.DESPESA);

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(UUID.randomUUID());
            return cat;
        });

        CategoryResponse response = categoryService.create(user.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.nome()).isEqualTo("Investimentos");
        assertThat(response.tipo()).isEqualTo(TipoCategoria.DESPESA);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void create_trimsNome() {
        User user = buildUser();
        CategoryRequest request = new CategoryRequest("  Lazer  ", TipoCategoria.DESPESA);

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.create(user.getId(), request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Lazer");
    }

    @Test
    void update_modifiesFieldsAndReturnsResponse() {
        User user = buildUser();
        Category category = buildCategory(user, "Original", TipoCategoria.DESPESA);
        CategoryRequest request = new CategoryRequest("Atualizada", TipoCategoria.RECEITA);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.update(user.getId(), category.getId(), request);

        assertThat(response.nome()).isEqualTo("Atualizada");
        assertThat(response.tipo()).isEqualTo(TipoCategoria.RECEITA);
        assertThat(category.getNome()).isEqualTo("Atualizada");
        assertThat(category.getTipo()).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void update_whenCategoryNotFoundOrNotOwned_throws() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        CategoryRequest request = new CategoryRequest("X", TipoCategoria.DESPESA);

        when(categoryRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(user.getId(), id, request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_removesCategoryWhenNotInUse() {
        User user = buildUser();
        Category category = buildCategory(user, "Lazer", TipoCategoria.DESPESA);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(category.getId())).thenReturn(false);
        when(budgetRepository.existsByCategoryId(category.getId())).thenReturn(false);

        categoryService.delete(user.getId(), category.getId());

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_whenCategoryNotFound_throws() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(user.getId(), id))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_whenInUseByTransaction_throwsCategoryInUse() {
        User user = buildUser();
        Category category = buildCategory(user, "Lazer", TipoCategoria.DESPESA);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(category.getId())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(user.getId(), category.getId()))
                .isInstanceOf(CategoryInUseException.class)
                .hasMessageContaining(category.getId().toString());

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_whenInUseByBudget_throwsCategoryInUse() {
        User user = buildUser();
        Category category = buildCategory(user, "Lazer", TipoCategoria.DESPESA);

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(category.getId())).thenReturn(false);
        when(budgetRepository.existsByCategoryId(category.getId())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(user.getId(), category.getId()))
                .isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private static User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .nome("Test")
                .email("test@example.com")
                .senha("hash")
                .build();
    }

    private static Category buildCategory(User user, String nome, TipoCategoria tipo) {
        return Category.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .tipo(tipo)
                .user(user)
                .build();
    }
}
