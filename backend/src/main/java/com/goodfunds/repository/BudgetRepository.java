package com.goodfunds.repository;

import com.goodfunds.domain.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndAnoAndMes(UUID userId, Integer ano, Integer mes);

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndAnoAndMesOrderByCategoryNomeAsc(UUID userId, Integer ano, Integer mes);

    Page<Budget> findByUserIdAndAnoAndMes(UUID userId, Integer ano, Integer mes, Pageable pageable);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryIdAndAnoAndMes(UUID userId, UUID categoryId, Integer ano, Integer mes);

    boolean existsByCategoryId(UUID categoryId);

    @Query("select coalesce(sum(b.limite), 0) from Budget b where b.user.id = :userId and b.ano = :ano and b.mes = :mes")
    BigDecimal sumLimiteByUserIdAndAnoAndMes(@Param("userId") UUID userId,
                                            @Param("ano") Integer ano,
                                            @Param("mes") Integer mes);
}
