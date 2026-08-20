package com.melodyHub.controller.artist;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.service.artist.PublicArtistService;
import com.melodyHub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Public artist browsing. Mapped to /api/artists/* (plural), distinct from the
 * authenticated /api/artist/* servlet.
 */
public class PublicArtistServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

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

            if ("/search".equals(path)) {
                writeJson(response, HttpServletResponse.SC_OK,
                        publicArtistService.search(request.getParameter("q")));
                return;
            }

            String slug = getSlug(path);
            if (slug != null && path.equals("/" + slug)) {
                handleDetail(request, response, slug);
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

    private void handleDetail(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException {
        var artist = publicArtistService.getBySlug(slug, currentUserId(request).orElse(null));
        if (artist.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, artist.get());
            return;
        }
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "ARTIST_NOT_FOUND", "Artist was not found");
    }

    private Optional<Integer> currentUserId(HttpServletRequest request) {
        String token = getBearerToken(request);
        if (token == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(JwtUtil.getUserIdFromToken(token));
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return Optional.empty();
        }
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
}
