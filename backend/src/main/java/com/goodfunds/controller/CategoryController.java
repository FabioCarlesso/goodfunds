package com.goodfunds.controller;

import com.goodfunds.config.OpenApiConfig;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.dto.CategoryRequest;
import com.goodfunds.dto.CategoryResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "CRUD de categorias de receitas e despesas do usuario.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Lista as categorias do usuario, opcionalmente filtrando por tipo.")
    public List<CategoryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) TipoCategoria tipo) {
        return categoryService.list(principal.getUserId(), tipo);
    }

    @PostMapping
    @Operation(summary = "Cria uma nova categoria.")
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(principal.getUserId(), request);
        URI location = UriComponentsBuilder.fromPath("/categories/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria existente.")
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma categoria que nao esteja em uso.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        categoryService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
