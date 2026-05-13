package com.goodfunds.repository;

import com.goodfunds.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserId(UUID userId);

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
}
