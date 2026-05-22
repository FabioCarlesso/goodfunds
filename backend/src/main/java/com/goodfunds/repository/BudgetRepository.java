package com.goodfunds.repository;

import com.goodfunds.domain.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserId(UUID userId);

    List<Budget> findByUserIdAndAnoAndMes(UUID userId, Integer ano, Integer mes);

    Page<Budget> findByUserIdAndAnoAndMes(UUID userId, Integer ano, Integer mes, Pageable pageable);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryIdAndAnoAndMes(UUID userId, UUID categoryId, Integer ano, Integer mes);

    boolean existsByCategoryId(UUID categoryId);
}
