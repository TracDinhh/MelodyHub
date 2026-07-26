package com.melodyHub.controller.artist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.service.artist.PublicArtistService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Public artist browsing. Mapped to /api/artists/* (plural), distinct from the
 * authenticated /api/artist/* servlet.
 */
public class PublicArtistServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private PublicArtistService publicArtistService;

    @Override
    public void init() throws ServletException {
        publicArtistService = new PublicArtistService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);

            if ("/".equals(path)) {
                handleList(request, response);
                return;
            }

            String slug = getSlug(path);
            if (slug != null && path.equals("/" + slug)) {
                handleDetail(response, slug);
                return;
            }

            String songsSlug = getSongsSlug(path);
            if (songsSlug != null) {
                handleSongs(request, response, songsSlug);
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Artist endpoint was not found");
        } catch (InvalidQueryParamException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Database error occurred");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }
        writeJson(response, HttpServletResponse.SC_OK,
                publicArtistService.list(page, size, request.getParameter("q")));
    }

    private void handleDetail(HttpServletResponse response, String slug) throws IOException, SQLException {
        var artist = publicArtistService.getBySlug(slug);
        if (artist.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, artist.get());
            return;
        }
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "ARTIST_NOT_FOUND", "Artist was not found");
    }

    private void handleSongs(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        var songs = publicArtistService.getSongsBySlug(slug, page, size);
        if (songs.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, songs.get());
            return;
        }
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "ARTIST_NOT_FOUND", "Artist was not found");
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

    private String getSongsSlug(String path) {
        String prefix = "/";
        String suffix = "/songs";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }
        String slug = path.substring(prefix.length(), path.length() - suffix.length());
        if (slug.isBlank() || slug.contains("/")) {
            return null;
        }
        return slug;
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
