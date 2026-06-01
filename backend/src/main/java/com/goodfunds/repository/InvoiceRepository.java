package com.goodfunds.repository;

import com.goodfunds.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByUserId(UUID userId);

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    List<Invoice> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);
}
