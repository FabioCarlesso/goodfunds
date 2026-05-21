package com.goodfunds.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(),
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalido"));

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Erro de validacao",
                "Requisicao invalida",
                "validation-error",
                instanceUri(request));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido",
                "O corpo da requisicao esta ausente ou possui JSON invalido",
                "invalid-request-body",
                instanceUri(request));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Metodo nao suportado",
                "Metodo HTTP nao suportado para este recurso",
                "method-not-allowed",
                instanceUri(request));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteudo nao suportado",
                "Content-Type nao suportado para este recurso",
                "unsupported-media-type",
                instanceUri(request));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.NOT_FOUND,
                "Recurso nao encontrado",
                "O recurso solicitado nao existe neste servidor",
                "resource-not-found",
                instanceUri(request));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.NOT_FOUND, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String parameter = (ex instanceof MethodArgumentTypeMismatchException matme)
                ? matme.getName()
                : ex.getPropertyName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valor valido";

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Erro de validacao",
                "Requisicao invalida",
                "validation-error",
                instanceUri(request));
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(parameter != null ? parameter : "parametro",
                "valor invalido, esperado tipo " + requiredType);
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Erro de validacao",
                "Requisicao invalida",
                "validation-error",
                instanceUri(request));
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "parametro obrigatorio ausente");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Erro de validacao",
                "Requisicao invalida",
                "validation-error",
                instanceUri(request));
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getRequestPartName(), "parte obrigatoria ausente");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ProblemDetail handleEmailInUse(EmailAlreadyInUseException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.CONFLICT,
                "E-mail ja cadastrado",
                ex.getMessage(),
                "email-already-in-use",
                instanceUri(request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                ex.getTitle(),
                ex.getMessage(),
                ex.getType(),
                instanceUri(request));
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ProblemDetail handleCategoryInUse(CategoryInUseException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Categoria em uso",
                ex.getMessage(),
                "category-in-use",
                instanceUri(request));
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    public ProblemDetail handleBudgetAlreadyExists(BudgetAlreadyExistsException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Orcamento ja existe",
                ex.getMessage(),
                "budget-already-exists",
                instanceUri(request));
    }

    @ExceptionHandler(InvalidTransactionFilterException.class)
    public ProblemDetail handleInvalidFilter(InvalidTransactionFilterException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Filtro invalido",
                ex.getMessage(),
                "invalid-filter",
                instanceUri(request));
    }

    @ExceptionHandler(InvalidInvoiceFileException.class)
    public ProblemDetail handleInvalidInvoiceFile(InvalidInvoiceFileException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Arquivo de fatura invalido",
                ex.getMessage(),
                "invalid-invoice-file",
                instanceUri(request));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Arquivo muito grande",
                "Tamanho do arquivo excede o limite permitido",
                "max-upload-size-exceeded",
                instanceUri(request));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ProblemDetail handleBadCredentials(AuthenticationException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Falha de autenticacao",
                "Credenciais invalidas",
                "authentication-failed",
                instanceUri(request));
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex, HttpServletRequest request) {
        return createProblem(
                HttpStatus.FORBIDDEN,
                "Usuario desabilitado",
                "Sua conta esta desabilitada",
                "user-disabled",
                instanceUri(request));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado",
                "internal-server-error",
                instanceUri(request));
    }

    private ProblemDetail createProblem(HttpStatus status, String title, String detail, String type, URI instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:goodfunds:problem:" + type));
        problem.setInstance(instance);
        return problem;
    }

    private URI instanceUri(HttpServletRequest request) {
        return URI.create(request.getRequestURI());
    }

    private URI instanceUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        return null;
    }
}
