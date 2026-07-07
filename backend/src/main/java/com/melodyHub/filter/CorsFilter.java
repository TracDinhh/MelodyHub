package com.melodyHub.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyHub.config.AppConfig;
import com.melodyHub.dto.response.ErrorResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CorsFilter implements Filter {
    private static final String CORS_ALLOWED_ORIGIN = "cors.allowed-origin";
    private static final String DEFAULT_ALLOWED_ORIGIN = "*";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String OPTIONS_METHOD = "OPTIONS";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        addCorsHeaders(httpRequest, httpResponse);

        if (OPTIONS_METHOD.equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        try {
            chain.doFilter(request, response);
        } catch (RuntimeException exception) {
            if (httpResponse.isCommitted()) {
                throw exception;
            }

            writeError(
                    httpResponse,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR",
                    "Internal server error occurred"
            );
        }
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String allowedOrigin = resolveAllowedOrigin(request);

        response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");

        if (!DEFAULT_ALLOWED_ORIGIN.equals(allowedOrigin)) {
            response.setHeader("Vary", "Origin");
        }
    }

    private String resolveAllowedOrigin(HttpServletRequest request) {
        String configuredOrigin = AppConfig.get(CORS_ALLOWED_ORIGIN);
        if (configuredOrigin == null || configuredOrigin.isBlank()) {
            return DEFAULT_ALLOWED_ORIGIN;
        }

        String trimmedOrigin = configuredOrigin.trim();
        if (DEFAULT_ALLOWED_ORIGIN.equals(trimmedOrigin)) {
            return DEFAULT_ALLOWED_ORIGIN;
        }

        String requestOrigin = request.getHeader("Origin");
        String[] allowedOrigins = trimmedOrigin.split(",");
        for (String allowedOrigin : allowedOrigins) {
            String safeAllowedOrigin = allowedOrigin.trim();
            if (!safeAllowedOrigin.isBlank() && safeAllowedOrigin.equals(requestOrigin)) {
                return requestOrigin;
            }
        }

        return allowedOrigins[0].trim();
    }

    private void writeError(HttpServletResponse response, int statusCode, String code, String message)
            throws IOException {
        response.setStatus(statusCode);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(CONTENT_TYPE_JSON);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
