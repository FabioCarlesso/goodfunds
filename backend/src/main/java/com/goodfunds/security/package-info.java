/**
 * Configuracao de seguranca e autenticacao JWT do Goodfunds.
 *
 * <p>Reune {@code SecurityFilterChain}, filtros JWT, encoders de senha e
 * componentes auxiliares de Spring Security. A emissao de tokens tem
 * expiracao de 24h, sem refresh token no MVP.</p>
 */
package com.goodfunds.security;
