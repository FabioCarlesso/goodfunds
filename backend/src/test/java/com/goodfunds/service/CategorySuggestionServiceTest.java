package com.goodfunds.service;

import com.goodfunds.config.CategoryRulesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategorySuggestionServiceTest {

    private CategorySuggestionService service;

    @BeforeEach
    void setUp() {
        CategoryRulesProperties props = new CategoryRulesProperties();

        CategoryRulesProperties.Rule alimentacao = new CategoryRulesProperties.Rule();
        alimentacao.setKeywords(List.of("ifood", "rappi", "padaria", "mercado"));
        alimentacao.setCategory("Alimentacao");

        CategoryRulesProperties.Rule transporte = new CategoryRulesProperties.Rule();
        transporte.setKeywords(List.of("uber", "99", "cabify", "posto"));
        transporte.setCategory("Transporte");

        CategoryRulesProperties.Rule lazer = new CategoryRulesProperties.Rule();
        lazer.setKeywords(List.of("netflix", "spotify", "disney"));
        lazer.setCategory("Lazer");

        props.setRules(List.of(alimentacao, transporte, lazer));
        service = new CategorySuggestionService(props);
    }

    @Test
    void suggest_alimentacaoKeywords_returnAlimentacao() {
        assertThat(service.suggest("IFOOD BURGUER")).contains("Alimentacao");
        assertThat(service.suggest("PADARIA CENTRAL")).contains("Alimentacao");
        assertThat(service.suggest("MERCADO LIVRE")).contains("Alimentacao");
        assertThat(service.suggest("RAPPI DELIVERY")).contains("Alimentacao");
    }

    @Test
    void suggest_transporteKeywords_returnTransporte() {
        assertThat(service.suggest("UBER TRIP")).contains("Transporte");
        assertThat(service.suggest("POSTO SHELL")).contains("Transporte");
        assertThat(service.suggest("CABIFY CORRIDA")).contains("Transporte");
    }

    @Test
    void suggest_lazerKeywords_returnLazer() {
        assertThat(service.suggest("NETFLIX MENSALIDADE")).contains("Lazer");
        assertThat(service.suggest("SPOTIFY PREMIUM")).contains("Lazer");
    }

    @Test
    void suggest_unknownDescription_returnsEmpty() {
        assertThat(service.suggest("ACADEMIA FIT")).isEmpty();
        assertThat(service.suggest("SALARIO MENSAL")).isEmpty();
    }

    @Test
    void suggest_nullDescription_returnsEmpty() {
        assertThat(service.suggest(null)).isEmpty();
    }

    @Test
    void suggest_blankDescription_returnsEmpty() {
        assertThat(service.suggest("   ")).isEmpty();
        assertThat(service.suggest("")).isEmpty();
    }

    @Test
    void suggest_isCaseInsensitive() {
        assertThat(service.suggest("iFood Burguer")).contains("Alimentacao");
        assertThat(service.suggest("Netflix Premium")).contains("Lazer");
        assertThat(service.suggest("UBER TRIP")).contains("Transporte");
    }

    @Test
    void suggest_firstMatchingRuleWins() {
        // Description contains keywords from two rules; first rule in list wins.
        assertThat(service.suggest("UBER MERCADO")).contains("Alimentacao");
    }
}
