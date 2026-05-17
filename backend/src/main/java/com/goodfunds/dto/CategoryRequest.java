package com.goodfunds.dto;

import com.goodfunds.domain.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotNull TipoCategoria tipo
) {
}
