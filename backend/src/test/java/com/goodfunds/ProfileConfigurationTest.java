package com.goodfunds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void defaultProfile_isDev() {
        contextRunner.run(context -> {
            Environment environment = context.getEnvironment();

            assertThat(environment.getProperty("spring.profiles.active")).isEqualTo("dev");
        });
    }

    @Test
    void devProfile_usesPostgreSqlWithFlywayManagedSchema() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    Environment environment = context.getEnvironment();

                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://localhost:5432/goodfunds");
                    assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                            .isEqualTo("org.postgresql.Driver");
                    assertThat(environment.getProperty("spring.datasource.username"))
                            .isEqualTo("goodfunds");
                    assertThat(environment.getProperty("spring.datasource.password"))
                            .isEqualTo("goodfunds");
                    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                            .isEqualTo("none");
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                            .isTrue();
                });
    }

    @Test
    void testProfile_usesH2InPostgreSqlCompatibilityModeWithFlywayManagedSchema() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    Environment environment = context.getEnvironment();

                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:h2:mem:goodfunds;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
                    assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                            .isEqualTo("org.h2.Driver");
                    assertThat(environment.getProperty("spring.datasource.username"))
                            .isEqualTo("sa");
                    assertThat(environment.getProperty("spring.datasource.password"))
                            .isEmpty();
                    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                            .isEqualTo("none");
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                            .isTrue();
                });
    }
}
