package com.goodfunds.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailResponseWriter responseWriter;

    public ProblemDetailAuthenticationEntryPoint(ProblemDetailResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        responseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "unauthenticated",
                "Nao autenticado",
                "Token de autenticacao ausente ou invalido");
    }
}
