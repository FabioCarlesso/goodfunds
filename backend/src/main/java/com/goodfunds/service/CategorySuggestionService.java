package com.goodfunds.service;

import com.goodfunds.config.CategoryRulesProperties;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sugere uma categoria para uma transacao com base em palavras-chave da descricao,
 * conforme as regras definidas em {@code category-rules.yml}.
 *
 * <p>O match e feito por <strong>token</strong> (palavra inteira), nao por substring:
 * a descricao e quebrada em tokens alfanumericos e uma regra so e aplicada quando uma
 * de suas palavras-chave coincide exatamente com um token. Isso evita falsos positivos
 * de keywords curtas/genericas (ex.: {@code "99"} dentro de {@code "MAGAZINE 1999"} ou
 * {@code "posto"} dentro de {@code "COMPOSTO"}). As palavras-chave devem ser tokens
 * unicos; espacos as tornam inalcancaveis.
 */
@Service
public class CategorySuggestionService {

    /** Separa a descricao em tokens: qualquer sequencia que nao seja letra/numero (Unicode-aware). */
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final CategoryRulesProperties properties;

    public CategorySuggestionService(CategoryRulesProperties properties) {
        this.properties = properties;
    }

    public Optional<String> suggest(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }
        Set<String> tokens = tokenize(descricao);
        return properties.getRules().stream()
                .filter(rule -> matchesAnyKeyword(rule, tokens))
                .map(CategoryRulesProperties.Rule::getCategory)
                .findFirst();
    }

    private static Set<String> tokenize(String descricao) {
        return Arrays.stream(TOKEN_SEPARATOR.split(descricao.toLowerCase(Locale.ROOT)))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private static boolean matchesAnyKeyword(CategoryRulesProperties.Rule rule, Set<String> tokens) {
        return rule.getKeywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(tokens::contains);
    }
}
