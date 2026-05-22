package com.goodfunds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Expoe um {@link Clock} como bean para que componentes que dependem da data/hora
 * corrente (ex.: a engine de estimativas) possam ser testados com um relogio fixo.
 *
 * <p>O relogio e fixado no fuso de negocio {@code America/Sao_Paulo} em vez de
 * {@code systemDefaultZone()}, para que o calculo de "hoje" (mes corrente,
 * {@code diasDecorridos} e limite do periodo fechado) nao dependa do fuso configurado
 * no servidor. Assim a borda de virada de mes acompanha o relogio local do usuario
 * brasileiro, e nao a do host.</p>
 */
@Configuration
public class ClockConfig {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
