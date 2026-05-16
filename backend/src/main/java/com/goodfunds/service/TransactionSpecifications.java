package com.goodfunds.service;

import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    static Specification<Transaction> hasCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Transaction> hasCategoryTipo(TipoCategoria tipo) {
        if (tipo == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("tipo"), tipo);
    }

    static Specification<Transaction> inMonth(YearMonth ref) {
        if (ref == null) {
            return null;
        }
        LocalDate first = ref.atDay(1);
        LocalDate last = ref.atEndOfMonth();
        return (root, query, cb) -> cb.between(root.get("data"), first, last);
    }

    static Specification<Transaction> dateFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("data"), from);
    }

    static Specification<Transaction> dateTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("data"), to);
    }
}
