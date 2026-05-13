/**
 * Excecoes de dominio e tratamento global de erros do Goodfunds.
 *
 * <p>Inclui o {@code GlobalExceptionHandler} ({@code @RestControllerAdvice})
 * que traduz excecoes para {@code ProblemDetail} (RFC 7807) e as excecoes
 * customizadas usadas pelas camadas de servico e controller.</p>
 */
package com.goodfunds.exception;
