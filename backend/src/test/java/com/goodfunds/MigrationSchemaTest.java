package com.goodfunds;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MigrationSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    // --- users ---

    @Test
    void users_uniqueEmail_rejectsDuplicate() {
        insertUser("dup-email@example.com");
        assertThatThrownBy(() -> insertUser("dup-email@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void users_invalidRole_isRejected() {
        assertThatThrownBy(() ->
                jdbc.update(
                        "INSERT INTO users (id, nome, email, senha, role) VALUES (?, 'X', 'bad-role@example.com', 'h', 'INVALID')",
                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- transactions ---

    @Test
    void transactions_zeroValor_isRejected() {
        UUID userId = insertUser("tx-zero@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() -> insertTransaction(userId, catId, BigDecimal.ZERO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transactions_negativeValor_isRejected() {
        UUID userId = insertUser("tx-neg@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() -> insertTransaction(userId, catId, new BigDecimal("-1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transactions_updatedAt_isSetOnInsert() {
        UUID userId = insertUser("tx-updatedat@example.com");
        UUID catId = insertCategory(userId);
        UUID txId = insertTransaction(userId, catId, new BigDecimal("99.90"));

        Object updatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM transactions WHERE id = ?", Object.class, txId);
        assertThat(updatedAt).isNotNull();
    }

    // --- budgets ---

    @Test
    void budgets_uniqueConstraint_rejectsDuplicate() {
        UUID userId = insertUser("bgt-dup@example.com");
        UUID catId = insertCategory(userId);
        insertBudget(userId, catId, 3, 2025);
        assertThatThrownBy(() -> insertBudget(userId, catId, 3, 2025))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void budgets_mesAbove12_isRejected() {
        UUID userId = insertUser("bgt-mes13@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() -> insertBudget(userId, catId, 13, 2025))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void budgets_mesZero_isRejected() {
        UUID userId = insertUser("bgt-mes0@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() -> insertBudget(userId, catId, 0, 2025))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void budgets_anoBelow2000_isRejected() {
        UUID userId = insertUser("bgt-ano@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() -> insertBudget(userId, catId, 1, 1999))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void budgets_negativeLimite_isRejected() {
        UUID userId = insertUser("bgt-limite@example.com");
        UUID catId = insertCategory(userId);
        assertThatThrownBy(() ->
                jdbc.update(
                        "INSERT INTO budgets (id, limite, category_id, mes, ano, user_id) VALUES (?, -10.00, ?, 1, 2025, ?)",
                        UUID.randomUUID(), catId, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- cascade ---

    @Test
    void deletingUser_cascadesToCategories() {
        UUID userId = insertUser("cascade@example.com");
        insertCategory(userId);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE user_id = ?", Integer.class, userId);
        assertThat(count).isZero();
    }

    // --- helpers ---

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, nome, email, senha) VALUES (?, 'Test', ?, 'bcrypt-hash')",
                id, email);
        return id;
    }

    private UUID insertCategory(UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO categories (id, nome, tipo, user_id) VALUES (?, 'Cat', 'DESPESA', ?)",
                id, userId);
        return id;
    }

    private UUID insertTransaction(UUID userId, UUID categoryId, BigDecimal valor) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO transactions (id, descricao, valor, data, forma_pagamento, category_id, user_id) " +
                "VALUES (?, 'Descricao', ?, ?, 'PIX', ?, ?)",
                id, valor, LocalDate.now(), categoryId, userId);
        return id;
    }

    private void insertBudget(UUID userId, UUID categoryId, int mes, int ano) {
        jdbc.update(
                "INSERT INTO budgets (id, limite, category_id, mes, ano, user_id) VALUES (?, 500.00, ?, ?, ?, ?)",
                UUID.randomUUID(), categoryId, mes, ano, userId);
    }
}
