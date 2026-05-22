package com.goodfunds.repository;

import com.goodfunds.domain.Transaction;
import com.goodfunds.repository.projection.CategoryAmount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    List<Transaction> findByInvoiceId(UUID invoiceId);

    @Query("""
            select new com.goodfunds.repository.projection.CategoryAmount(t.category.id, sum(t.valor))
            from Transaction t
            where t.user.id = :userId and t.data between :start and :end
            group by t.category.id
            """)
    List<CategoryAmount> sumByCategoryAndPeriod(@Param("userId") UUID userId,
                                                @Param("start") LocalDate start,
                                                @Param("end") LocalDate end);

    @Modifying
    @Query("delete from Transaction t where t.invoice.id = :invoiceId")
    int deleteByInvoiceId(@Param("invoiceId") UUID invoiceId);

    @Override
    @EntityGraph(attributePaths = {"category", "invoice"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
