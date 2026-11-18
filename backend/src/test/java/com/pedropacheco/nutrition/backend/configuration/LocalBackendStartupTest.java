package com.pedropacheco.nutrition.backend.configuration;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalBackendStartupTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;

    @Test
    void startsAnHttpServerWithoutConsoleInputAndServesThePublicHealthEndpoint() {
        assertTrue(context.getBeansOfType(TerminalRunner.class).isEmpty());
        var client = RestClient.create("http://127.0.0.1:" + port);
        var response = client.get().uri("/api/health").retrieve().toEntity(Map.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("status", "UP"), response.getBody());
    }
}
