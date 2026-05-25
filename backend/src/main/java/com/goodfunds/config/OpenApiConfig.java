package com.goodfunds.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Springdoc/OpenAPI 3.
 *
 * <p>Define os metadados da especificacao (titulo, descricao, versao) e registra o
 * esquema de seguranca {@code bearerAuth} (HTTP Bearer com formato JWT). Com o esquema
 * declarado, o Swagger UI exibe o botao <em>Authorize</em> para informar o token; os
 * controllers protegidos referenciam o esquema via
 * {@code @SecurityRequirement(name = "bearerAuth")}, enquanto {@code /auth/**} permanece
 * publico (sem o requisito).</p>
 *
 * <p>O Swagger UI ({@code /swagger-ui.html}) e a especificacao ({@code /v3/api-docs})
 * ficam liberados no {@link com.goodfunds.security.SecurityConfig} e habilitados em dev;
 * em producao sao desativados via {@code springdoc.*.enabled=false}.</p>
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI goodfundsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Goodfunds API")
                        .description("API REST do Goodfunds — sistema de controle financeiro pessoal "
                                + "(faturas, transacoes, categorias, orcamentos, relatorios e estimativas).")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido em POST /auth/login. "
                                        + "Informe apenas o token; o prefixo \"Bearer\" e adicionado automaticamente.")));
    }
}
