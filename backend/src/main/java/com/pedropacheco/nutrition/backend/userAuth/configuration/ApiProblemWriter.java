package com.pedropacheco.nutrition.backend.userAuth.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/** Writes Problem Details for security failures that occur before Spring MVC. */
@Component
public class ApiProblemWriter {
    private final ObjectMapper mapper;

    public ApiProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatusCode status, String detail) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), Map.of(
                "type", problem.getType().toString(), "title", problem.getTitle(),
                "status", problem.getStatus(), "detail", problem.getDetail(),
                "instance", problem.getInstance().toString()));
    }
}
