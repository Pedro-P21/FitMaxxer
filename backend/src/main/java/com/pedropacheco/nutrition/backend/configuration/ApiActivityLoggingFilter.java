package com.pedropacheco.nutrition.backend.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs API functionality and outcomes without recording request or response data. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiActivityLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiActivityLoggingFilter.class);
    private static final AtomicLong REQUEST_NUMBER = new AtomicLong();
    private static final List<Action> ACTIONS = List.of(
            action("GET", "/api/health", "Check API health"),
            action("POST", "/api/auth/register", "Register account"),
            action("POST", "/api/auth/login", "Login"),
            action("GET", "/api/profiles", "View nutritional profile"),
            action("POST", "/api/profiles/personal-data", "Save personal data and calculate targets"),
            action("PUT", "/api/profiles", "Edit nutritional profile"),
            action("GET", "/api/foods", "Search food catalog"),
            action("GET", "/api/daily-log/today", "View today's log"),
            action("GET", "/api/daily-log/history", "View log history"),
            action("POST", "/api/daily-log/today/items", "Log food"),
            action("POST", "/api/daily-log/today/recipe-items", "Log recipe"),
            action("POST", "/api/daily-log/today/meal-copies", "Copy historical meal"),
            action("PATCH", "/api/daily-log/today/items/[^/]+/quantity", "Change logged quantity"),
            action("PATCH", "/api/daily-log/today/items/[^/]+/meal", "Move logged item"),
            action("DELETE", "/api/daily-log/today/items/[^/]+", "Delete logged item"),
            action("POST", "/api/recipes", "Create recipe"),
            action("GET", "/api/recipes", "Browse recipes"),
            action("GET", "/api/recipes/[^/]+", "View recipe"));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !path(request).startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = path(request);
        String functionality = ACTIONS.stream()
                .filter(action -> action.method().equals(request.getMethod())
                        && action.path().matcher(path).matches())
                .map(Action::label).findFirst().orElse("Other API request");
        long number = REQUEST_NUMBER.incrementAndGet();
        long started = System.nanoTime();
        boolean returned = false;
        log.info("API #{} START {}", number, functionality);
        try {
            chain.doFilter(request, response);
            returned = true;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (returned) {
                int status = response.getStatus();
                String outcome = status >= 500 ? "FAILED" : status >= 400 ? "REJECTED" : "COMPLETED";
                log.info("API #{} {} {} status={} durationMs={}",
                        number, outcome, functionality, status, durationMs);
            } else {
                // An escaping exception may be converted to an HTTP response after this filter.
                log.warn("API #{} FAILED {} before response completed durationMs={}",
                        number, functionality, durationMs);
            }
        }
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static Action action(String method, String path, String label) {
        return new Action(method, Pattern.compile(path), label);
    }

    private record Action(String method, Pattern path, String label) {}
}
