package com.goodfunds.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o arquivo real {@code category-rules.yml} liga corretamente em
 * {@link CategoryRulesProperties}, pegando typos de chave/indentacao que so seriam
 * percebidos em runtime.
 */
class CategoryRulesPropertiesBindingTest {

    @Test
    void categoryRulesYmlBindsToProperties() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("category-rules", new ClassPathResource("category-rules.yml"));

        MutablePropertySources propertySources = new MutablePropertySources();
        sources.forEach(propertySources::addLast);

        CategoryRulesProperties props = new Binder(ConfigurationPropertySources.from(propertySources))
                .bind("app.categories", CategoryRulesProperties.class)
                .orElseGet(CategoryRulesProperties::new);

        assertThat(props.getRules()).isNotEmpty();
        assertThat(props.getRules())
                .extracting(CategoryRulesProperties.Rule::getCategory)
                .contains("Alimentacao", "Transporte", "Lazer", "Saude", "Educacao", "Moradia");
        assertThat(props.getRules())
                .allSatisfy(rule -> assertThat(rule.getKeywords()).isNotEmpty());
    }
}
