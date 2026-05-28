package com.goodfunds.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailResponseWriter responseWriter;

    public ProblemDetailAccessDeniedHandler(ProblemDetailResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        responseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Acesso negado",
                "Acesso negado para este recurso");
    }
}
