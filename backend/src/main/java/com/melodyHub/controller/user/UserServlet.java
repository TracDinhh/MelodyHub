package com.melodyHub.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.request.ProfileUpdateRequest;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.auth.AuthService;
import com.melodyHub.service.auth.AuthorizationService;
import com.melodyHub.service.user.UserLibraryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Self-service user profile. Mapped to /api/users/*.
 */
public class UserServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuthService authService;
    private AuthorizationService authorizationService;
    private UserLibraryService userLibraryService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
        authorizationService = new AuthorizationService();
        userLibraryService = new UserLibraryService();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("PATCH".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
            return;
        }
        super.service(request, response);
    }

    private void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            if ("/me".equals(getPath(request))) {
                handleUpdateMe(request, response);
                return;
            }
            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "User endpoint was not found"
            );
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        } catch (IOException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_JSON",
                    "Request body is invalid"
            );
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!"/me/liked-songs".equals(getPath(request))) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User endpoint was not found");
                return;
            }

            int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
            int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
            if (size > MAX_SIZE) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", "size must not exceed " + MAX_SIZE);
                return;
            }
            int userId = authorizationService.requireAuthenticated(getBearerToken(request)).getId();
            PagedResponse<SongSummaryResponse> payload = userLibraryService.getLikedSongs(userId, page, size);
            writeJson(response, HttpServletResponse.SC_OK, payload);
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        updateLike(request, response, true);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        updateLike(request, response, false);
    }

    private void updateLike(HttpServletRequest request, HttpServletResponse response, boolean shouldLike) throws IOException {
        try {
            Integer songId = getLikedSongId(getPath(request));
            if (songId == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User endpoint was not found");
                return;
            }
            int userId = authorizationService.requireAuthenticated(getBearerToken(request)).getId();
            boolean liked = (shouldLike
                    ? userLibraryService.likeSong(userId, songId)
                    : userLibraryService.unlikeSong(userId, songId))
                    .orElseThrow(() -> new IllegalArgumentException("Song was not found"));
            writeJson(response, HttpServletResponse.SC_OK, java.util.Map.of("songId", songId, "liked", liked));
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            int status = "Song was not found".equals(exception.getMessage())
                    ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_BAD_REQUEST;
            writeError(response, status, status == HttpServletResponse.SC_NOT_FOUND ? "SONG_NOT_FOUND" : "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    private void handleUpdateMe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        ProfileUpdateRequest body = objectMapper.readValue(request.getInputStream(), ProfileUpdateRequest.class);
        writeJson(response, HttpServletResponse.SC_OK, authService.updateMyProfile(getBearerToken(request), body));
    }

    private String getPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
    }

    private String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private Integer getLikedSongId(String path) {
        String prefix = "/me/liked-songs/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String value = path.substring(prefix.length());
        if (value.isBlank() || value.contains("/")) {
            return null;
        }
        try {
            int songId = Integer.parseInt(value);
            return songId > 0 ? songId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int parsePositiveInt(String value, String name, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                throw new IllegalArgumentException(name + " must be a positive integer");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "INVALID_REQUEST",
                    "INVALID_DISPLAY_NAME",
                    "INVALID_EMAIL",
                    "INVALID_AVATAR_URL" -> HttpServletResponse.SC_BAD_REQUEST;
            case "MISSING_TOKEN",
                    "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
            case "EMAIL_EXISTS" -> HttpServletResponse.SC_CONFLICT;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
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
}
