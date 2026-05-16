package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.dto.TransactionRequest;
import com.goodfunds.dto.TransactionResponse;
import com.goodfunds.exception.CategoryNotFoundException;
import com.goodfunds.exception.TransactionNotFoundException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_persistsTransactionAndReturnsResponse() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao", TipoCategoria.DESPESA);
        TransactionRequest request = new TransactionRequest(
                "Mercado",
                new BigDecimal("125.50"),
                LocalDate.of(2026, 5, 10),
                FormaPagamento.PIX,
                category.getId());

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(UUID.randomUUID());
                    return tx;
                });

        TransactionResponse response = transactionService.create(user, request);

        assertThat(response.descricao()).isEqualTo("Mercado");
        assertThat(response.valor()).isEqualByComparingTo("125.50");
        assertThat(response.data()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.formaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.categoryNome()).isEqualTo("Alimentacao");
        assertThat(response.categoryTipo()).isEqualTo(TipoCategoria.DESPESA);
        assertThat(response.invoiceId()).isNull();

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getDescricao()).isEqualTo("Mercado");
    }

    @Test
    void create_trimsDescricao() {
        User user = buildUser();
        Category category = buildCategory(user, "Lazer", TipoCategoria.DESPESA);
        TransactionRequest request = new TransactionRequest(
                "  Cinema  ",
                new BigDecimal("30.00"),
                LocalDate.of(2026, 5, 1),
                FormaPagamento.DINHEIRO,
                category.getId());

        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                .thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.create(user, request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Cinema");
    }

    @Test
    void create_whenCategoryDoesNotBelongToUser_throwsCategoryNotFound() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(
                "Compra",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 5, 1),
                FormaPagamento.PIX,
                categoryId);

        when(categoryRepository.findByIdAndUserId(categoryId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(user, request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update_modifiesFieldsAndReturnsResponse() {
        User user = buildUser();
        Category oldCategory = buildCategory(user, "Alimentacao", TipoCategoria.DESPESA);
        Category newCategory = buildCategory(user, "Lazer", TipoCategoria.DESPESA);
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .descricao("Original")
                .valor(new BigDecimal("50.00"))
                .data(LocalDate.of(2026, 4, 1))
                .formaPagamento(FormaPagamento.DINHEIRO)
                .category(oldCategory)
                .user(user)
                .build();

        TransactionRequest request = new TransactionRequest(
                "Atualizado",
                new BigDecimal("80.00"),
                LocalDate.of(2026, 5, 1),
                FormaPagamento.PIX,
                newCategory.getId());

        when(transactionRepository.findByIdAndUserId(transaction.getId(), user.getId()))
                .thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserId(newCategory.getId(), user.getId()))
                .thenReturn(Optional.of(newCategory));

        TransactionResponse response = transactionService.update(user, transaction.getId(), request);

        assertThat(response.descricao()).isEqualTo("Atualizado");
        assertThat(response.valor()).isEqualByComparingTo("80.00");
        assertThat(response.data()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.formaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(response.categoryId()).isEqualTo(newCategory.getId());

        assertThat(transaction.getDescricao()).isEqualTo("Atualizado");
        assertThat(transaction.getCategory()).isSameAs(newCategory);
    }

    @Test
    void update_whenTransactionNotFoundOrNotOwnedByUser_throws() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(
                "x",
                new BigDecimal("1.00"),
                LocalDate.of(2026, 5, 1),
                FormaPagamento.PIX,
                UUID.randomUUID());

        when(transactionRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.update(user, id, request))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void update_whenCategoryDoesNotBelongToUser_throwsCategoryNotFound() {
        User user = buildUser();
        Category oldCategory = buildCategory(user, "Alimentacao", TipoCategoria.DESPESA);
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .descricao("desc")
                .valor(new BigDecimal("10.00"))
                .data(LocalDate.of(2026, 4, 1))
                .formaPagamento(FormaPagamento.PIX)
                .category(oldCategory)
                .user(user)
                .build();

        UUID otherCategoryId = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(
                "desc",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 4, 1),
                FormaPagamento.PIX,
                otherCategoryId);

        when(transactionRepository.findByIdAndUserId(transaction.getId(), user.getId()))
                .thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserId(otherCategoryId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.update(user, transaction.getId(), request))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void delete_removesTransaction() {
        User user = buildUser();
        Category category = buildCategory(user, "Outros", TipoCategoria.DESPESA);
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .descricao("x")
                .valor(BigDecimal.ONE)
                .data(LocalDate.of(2026, 5, 1))
                .formaPagamento(FormaPagamento.PIX)
                .category(category)
                .user(user)
                .build();

        when(transactionRepository.findByIdAndUserId(transaction.getId(), user.getId()))
                .thenReturn(Optional.of(transaction));

        transactionService.delete(user, transaction.getId());

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void delete_whenTransactionNotFound_throws() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(user, id))
                .isInstanceOf(TransactionNotFoundException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void search_buildsSpecificationAndMapsToResponse() {
        User user = buildUser();
        Category category = buildCategory(user, "Alimentacao", TipoCategoria.DESPESA);
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .descricao("Mercado")
                .valor(new BigDecimal("99.99"))
                .data(LocalDate.of(2026, 5, 10))
                .formaPagamento(FormaPagamento.PIX)
                .category(category)
                .user(user)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(tx), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionResponse> result = transactionService.search(
                user,
                YearMonth.of(2026, 5),
                category.getId(),
                TipoCategoria.DESPESA,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).descricao()).isEqualTo("Mercado");
        assertThat(result.getContent().get(0).categoryNome()).isEqualTo("Alimentacao");
    }

    @Test
    void search_withoutFilters_stillScopesByUser() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 5);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<TransactionResponse> result = transactionService.search(
                user, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
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
