package com.melodyHub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.service.song.SongService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class SongServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private SongService songService;

    @Override
    public void init() throws ServletException {
        songService = new SongService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleGetPage(request, response);
                return;
            }

            String slug = getSlug(path);
            if (slug != null) {
                handleGetBySlug(response, slug);
                return;
            }

            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "Song endpoint was not found"
            );
        } catch (InvalidQueryParamException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_QUERY_PARAM",
                    exception.getMessage()
            );
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        }
    }

    private void handleGetPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        writeJson(response, HttpServletResponse.SC_OK, songService.getPage(page, size));
    }

    private int parsePositiveInt(String value, String name, int defaultValue) throws InvalidQueryParamException {
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

    private void handleGetBySlug(HttpServletResponse response, String slug) throws IOException, SQLException {
        var song = songService.getBySlug(slug);
        if (song.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, song.get());
            return;
        }

        writeError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "SONG_NOT_FOUND",
                "Song was not found"
        );
    }

    private String getPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
    }

    private String getSlug(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty() || trimmed.contains("/")) {
            return null;
        }

        return trimmed;
    }

    private void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
        response.setStatus(statusCode);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(CONTENT_TYPE_JSON);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeError(HttpServletResponse response, int statusCode, String code, String message)
            throws IOException {
        writeJson(response, statusCode, new ErrorResponse(code, message));
    }

    private static final class InvalidQueryParamException extends Exception {
        private InvalidQueryParamException(String message) {
            super(message);
        }
    }
}
