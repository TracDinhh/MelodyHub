package com.melodyHub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Shared base for all JSON servlets. Centralizes the JSON {@link ObjectMapper}
 * (configured to emit ISO date strings, not timestamps), the {@code writeJson}
 * / {@code writeError} helpers, path extraction, bearer-token parsing, and
 * positive-integer query-param parsing that every servlet previously duplicated.
 */
public abstract class JsonServlet extends HttpServlet {
    protected static final String CONTENT_TYPE_JSON = "application/json";
    protected static final String BEARER_PREFIX = "Bearer ";

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected String getPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
    }

    /** Returns the raw bearer token, or {@code null} when the header is absent/blank. */
    protected String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    protected int parsePositiveInt(String value, String name, int defaultValue)
            throws InvalidQueryParamException {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new InvalidQueryParamException(name + " must be a positive integer");
        }

        if (parsed < 1) {
            throw new InvalidQueryParamException(name + " must be a positive integer");
        }

        return parsed;
    }

    protected void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
        response.setStatus(statusCode);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(CONTENT_TYPE_JSON);
        objectMapper.writeValue(response.getWriter(), body);
    }

    protected void writeError(HttpServletResponse response, int statusCode, String code, String message)
            throws IOException {
        writeJson(response, statusCode, new ErrorResponse(code, message));
    }

    protected void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    /** Thrown when a query/path parameter is present but invalid. Maps to HTTP 400. */
    protected static final class InvalidQueryParamException extends Exception {
        public InvalidQueryParamException(String message) {
            super(message);
        }
    }
}
