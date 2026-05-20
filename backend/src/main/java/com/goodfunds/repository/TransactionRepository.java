package com.goodfunds.repository;

import com.goodfunds.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    List<Transaction> findByInvoiceId(UUID invoiceId);

    long deleteByInvoiceId(UUID invoiceId);

    @Override
    @EntityGraph(attributePaths = {"category", "invoice"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
