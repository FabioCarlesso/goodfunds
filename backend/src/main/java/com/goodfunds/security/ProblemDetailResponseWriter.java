package com.goodfunds.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Serializa um {@link ProblemDetail} (RFC 7807) direto na {@link HttpServletResponse},
 * para uso dos handlers do filtro de seguranca ({@link ProblemDetailAuthenticationEntryPoint}
 * e {@link ProblemDetailAccessDeniedHandler}) que escrevem antes do dispatcher Spring MVC
 * e por isso nao passam pelo {@code @RestControllerAdvice}.
 */
@Component
public class ProblemDetailResponseWriter {

    private static final String TYPE_URN_PREFIX = "urn:goodfunds:problem:";

    private final ObjectMapper objectMapper;

    public ProblemDetailResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatus status,
                      String type,
                      String title,
                      String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_URN_PREFIX + type));
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
