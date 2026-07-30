package com.melodyHub.controller.song;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.dto.response.SongDetailResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.service.song.SongService;
import com.melodyHub.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SongServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_RELATED_SIZE = 8;
    private static final int MAX_RELATED_SIZE = 12;

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

            String relatedSegment = getRelatedSegment(path);
            if (relatedSegment != null) {
                handleGetRelated(request, response, relatedSegment);
                return;
            }

            String slug = getSlug(path);
            if (slug != null) {
                handleGetBySlug(request, response, slug);
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

        String titleQuery = request.getParameter("q");
        String genreSlug = request.getParameter("genre");
        writeJson(response, HttpServletResponse.SC_OK, songService.getPage(page, size, titleQuery, genreSlug));
    }

    private void handleGetBySlug(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException {
        Optional<SongDetailResponse> detail = songService.getDetail(slug, currentUserId(request).orElse(null));
        if (detail.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, detail.get());
            return;
        }

        writeError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "SONG_NOT_FOUND",
                "Song was not found"
        );
    }

    private void handleGetRelated(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException, InvalidQueryParamException {
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_RELATED_SIZE);
        if (size > MAX_RELATED_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_RELATED_SIZE);
        }

        List<SongSummaryResponse> related = songService.getRelated(slug, size);
        if (related == null) {
            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "SONG_NOT_FOUND",
                    "Song was not found"
            );
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("items", related));
    }

    private Optional<Integer> currentUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JwtUtil.getUserIdFromToken(token));
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return Optional.empty();
        }
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

    private String getRelatedSegment(String path) {
        String suffix = "/related";
        if (!path.endsWith(suffix)) {
            return null;
        }
        String slug = path.substring(1, path.length() - suffix.length());
        if (slug.isBlank() || slug.contains("/")) {
            return null;
        }
        return slug;
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
