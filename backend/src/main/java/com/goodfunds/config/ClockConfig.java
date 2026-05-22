package com.goodfunds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expoe um {@link Clock} como bean para que componentes que dependem da data/hora
 * corrente (ex.: a engine de estimativas) possam ser testados com um relogio fixo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
