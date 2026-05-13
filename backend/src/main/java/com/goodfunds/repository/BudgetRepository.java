package com.goodfunds.repository;

import com.goodfunds.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndAnoAndMes(UUID userId, Integer ano, Integer mes);

    Optional<Budget> findByUserIdAndCategoryIdAndAnoAndMes(UUID userId, UUID categoryId, Integer ano, Integer mes);
}
