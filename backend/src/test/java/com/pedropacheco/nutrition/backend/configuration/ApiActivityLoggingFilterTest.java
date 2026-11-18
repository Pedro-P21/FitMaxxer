package com.pedropacheco.nutrition.backend.configuration;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class ApiActivityLoggingFilterTest {
    private final ApiActivityLoggingFilter filter = new ApiActivityLoggingFilter();

    @Test
    void reportsLoginOutcomeWithoutLoggingCredentialsTokensOrBodies(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContent("{\"identifier\":\"private-user\",\"password\":\"private-password\"}".getBytes());
        request.addHeader("Authorization", "Bearer private-token");
        request.setQueryString("secret=private-query");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            response.setStatus(200);
            response.getWriter().write("{\"token\":\"private-response-token\"}");
        });
        assertThat(output.getAll()).contains("START Login", "COMPLETED Login status=200", "durationMs=")
                .doesNotContain("private-user", "private-password", "private-token", "private-query",
                        "private-response-token");
        assertThat(response.getContentAsString()).contains("private-response-token");
    }

    @Test
    void identifiesItemOperationAndRetainsRejectionStatus(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("PATCH", "/api/daily-log/today/items/123/meal");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> response.setStatus(409));
        assertThat(output.getAll()).contains("START Move logged item", "REJECTED Move logged item status=409");
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void escapingExceptionIsNotReportedAsSuccessful(CapturedOutput output) {
        var request = new MockHttpServletRequest("GET", "/api/profiles");
        assertThrows(ServletException.class, () -> filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> { throw new ServletException("private-internal-error"); }));
        assertThat(output.getAll()).contains("FAILED View nutritional profile before response completed")
                .doesNotContain("COMPLETED", "private-internal-error", "status=200");
    }

    @Test
    void skipsNonApiTraffic(CapturedOutput output) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/h2-console"), response,
                (req, res) -> response.setStatus(204));
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(output.getAll()).doesNotContain("API #");
    }
}
