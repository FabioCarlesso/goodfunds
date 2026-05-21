package com.goodfunds.service;

import com.goodfunds.config.CategoryRulesProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Sugere uma categoria para uma transacao com base em palavras-chave da descricao,
 * conforme as regras definidas em {@code category-rules.yml}.
 */
@Service
public class CategorySuggestionService {

    private final CategoryRulesProperties properties;

    public CategorySuggestionService(CategoryRulesProperties properties) {
        this.properties = properties;
    }

    public Optional<String> suggest(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }
        String lower = descricao.toLowerCase(Locale.ROOT);
        return properties.getRules().stream()
                .filter(rule -> rule.getKeywords().stream().anyMatch(lower::contains))
                .map(CategoryRulesProperties.Rule::getCategory)
                .findFirst();
    }
}
