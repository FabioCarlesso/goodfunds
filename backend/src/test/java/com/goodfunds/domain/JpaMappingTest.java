package com.goodfunds.domain;

import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaMappingTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void user_isPersistedWithDefaultsAndCreatedAt() {
        User user = userRepository.save(newUser("user-defaults@example.com"));

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("user-defaults@example.com");
        assertThat(reloaded.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void user_duplicateEmail_isRejected() {
        userRepository.saveAndFlush(newUser("dup-jpa@example.com"));
        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("dup-jpa@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void category_isPersistedWithUserRelation() {
        User user = userRepository.save(newUser("cat-owner@example.com"));
        Category category = categoryRepository.save(newCategory(user, "Alimentacao", TipoCategoria.DESPESA));

        entityManager.flush();
        entityManager.clear();

        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Alimentacao");
        assertThat(reloaded.getTipo()).isEqualTo(TipoCategoria.DESPESA);
        assertThat(reloaded.getUser().getId()).isEqualTo(user.getId());

        List<Category> ofUser = categoryRepository.findByUserId(user.getId());
        assertThat(ofUser).hasSize(1);
    }

    @Test
    void invoice_isPersistedWithYearMonthAndEnums() {
        User user = userRepository.save(newUser("inv-owner@example.com"));

        Invoice invoice = Invoice.builder()
                .arquivo("./uploads/" + user.getId() + "/fatura.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PENDENTE_PARSE)
                .mesReferencia(YearMonth.of(2026, 3))
                .totalValor(new BigDecimal("1234.56"))
                .user(user)
                .build();
        invoice = invoiceRepository.save(invoice);

        entityManager.flush();
        entityManager.clear();

        Invoice reloaded = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloaded.getOrigem()).isEqualTo(OrigemFatura.NUBANK);
        assertThat(reloaded.getStatus()).isEqualTo(StatusFatura.PENDENTE_PARSE);
        assertThat(reloaded.getMesReferencia()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(reloaded.getTotalValor()).isEqualByComparingTo("1234.56");
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void transaction_isPersistedWithRelationsAndAuditTimestamps() {
        User user = userRepository.save(newUser("tx-owner@example.com"));
        Category category = categoryRepository.save(newCategory(user, "Lazer", TipoCategoria.DESPESA));

        Transaction tx = Transaction.builder()
                .descricao("Cinema")
                .valor(new BigDecimal("45.00"))
                .data(LocalDate.of(2026, 4, 10))
                .formaPagamento(FormaPagamento.PIX)
                .category(category)
                .user(user)
                .build();
        tx = transactionRepository.save(tx);

        entityManager.flush();
        entityManager.clear();

        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getDescricao()).isEqualTo("Cinema");
        assertThat(reloaded.getValor()).isEqualByComparingTo("45.00");
        assertThat(reloaded.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(reloaded.getCategory().getId()).isEqualTo(category.getId());
        assertThat(reloaded.getInvoice()).isNull();
        assertThat(reloaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void transaction_linkedToInvoice_isPersistedWithInvoiceRelation() {
        User user = userRepository.save(newUser("tx-inv@example.com"));
        Category category = categoryRepository.save(newCategory(user, "Outros", TipoCategoria.DESPESA));
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .arquivo("./uploads/" + user.getId() + "/inv.pdf")
                .origem(OrigemFatura.ITAU)
                .status(StatusFatura.PROCESSADA)
                .mesReferencia(YearMonth.of(2026, 5))
                .totalValor(new BigDecimal("100.00"))
                .user(user)
                .build());

        Transaction tx = transactionRepository.save(Transaction.builder()
                .descricao("Compra no app")
                .valor(new BigDecimal("100.00"))
                .data(LocalDate.of(2026, 5, 1))
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(category)
                .invoice(invoice)
                .user(user)
                .build());

        entityManager.flush();
        entityManager.clear();

        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getInvoice()).isNotNull();
        assertThat(reloaded.getInvoice().getId()).isEqualTo(invoice.getId());
    }

    @Test
    void budget_uniqueConstraintRejectsDuplicateUserCategoryMesAno() {
        User user = userRepository.save(newUser("bgt-jpa@example.com"));
        Category category = categoryRepository.save(newCategory(user, "Mercado", TipoCategoria.DESPESA));

        budgetRepository.saveAndFlush(Budget.builder()
                .limite(new BigDecimal("500.00"))
                .category(category)
                .mes(6)
                .ano(2026)
                .user(user)
                .build());

        assertThatThrownBy(() -> budgetRepository.saveAndFlush(Budget.builder()
                .limite(new BigDecimal("600.00"))
                .category(category)
                .mes(6)
                .ano(2026)
                .user(user)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void budget_findByUserAnoMes_returnsPersistedRow() {
        User user = userRepository.save(newUser("bgt-find@example.com"));
        Category category = categoryRepository.save(newCategory(user, "Saude", TipoCategoria.DESPESA));

        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("250.00"))
                .category(category)
                .mes(7)
                .ano(2026)
                .user(user)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Budget> found = budgetRepository.findByUserIdAndAnoAndMes(user.getId(), 2026, 7);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getLimite()).isEqualByComparingTo("250.00");
    }

    private User newUser(String email) {
        return User.builder()
                .nome("Test User")
                .email(email)
                .senha("bcrypt-hash")
                .build();
    }

    private Category newCategory(User user, String nome, TipoCategoria tipo) {
        return Category.builder()
                .nome(nome)
                .tipo(tipo)
                .user(user)
                .build();
    }
}
