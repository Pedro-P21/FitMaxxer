package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userAuth.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CliStartupConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.location=file:./src/main/resources/application.properties")
            .withUserConfiguration(TerminalRunner.class)
            .withBean(UserAuthService.class, () -> mock(UserAuthService.class))
            .withBean(FitMaxxerMainMenu.class, () -> mock(FitMaxxerMainMenu.class))
            .withBean(CreateUserNutritionalProfileUI.class, () -> mock(CreateUserNutritionalProfileUI.class))
            .withBean(JwtDecoder.class, () -> mock(JwtDecoder.class));

    @Test
    void defaultApiModeDoesNotCreateAnInteractiveRunner() {
        context.run(application -> {
            assertThat(application).doesNotHaveBean(TerminalRunner.class);
            assertThat(application.getEnvironment().getProperty("server.address")).isEqualTo("127.0.0.1");
            assertThat(application.getEnvironment().getProperty("server.port")).isEqualTo("8080");
        });
    }

    @Test
    void cliProfileEnablesTheRunnerWithoutChangingServerBinding() {
        context.withPropertyValues("spring.profiles.active=cli").run(application -> {
            assertThat(application).hasSingleBean(TerminalRunner.class);
            assertThat(application.getEnvironment().getProperty("server.address")).isEqualTo("127.0.0.1");
        });
    }

    @Test
    void mobileDevelopmentProfileAllowsLanAccessWithoutInteractiveInput() {
        context.withPropertyValues("spring.profiles.active=mobile-dev").run(application -> {
            assertThat(application).doesNotHaveBean(TerminalRunner.class);
            assertThat(application.getEnvironment().getProperty("server.address")).isEqualTo("0.0.0.0");
        });
    }

    @Test
    void cliCanBeExplicitlyDisabledEvenWithTheCliProfile() {
        context.withPropertyValues("spring.profiles.active=cli", "app.cli.enabled=false")
                .run(application -> assertThat(application).doesNotHaveBean(TerminalRunner.class));
    }
}
