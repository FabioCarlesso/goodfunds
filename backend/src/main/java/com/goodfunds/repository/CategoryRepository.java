package com.goodfunds.repository;

import com.goodfunds.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goodfunds.domain.TipoCategoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserId(UUID userId);

    List<Category> findByUserIdOrderByNomeAsc(UUID userId);

    List<Category> findByUserIdAndTipoOrderByNomeAsc(UUID userId, TipoCategoria tipo);

    Page<Category> findByUserId(UUID userId, Pageable pageable);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    Optional<Category> findFirstByUserIdAndNomeIgnoreCase(UUID userId, String nome);
}
