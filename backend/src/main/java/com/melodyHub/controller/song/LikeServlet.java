package com.melodyHub.controller.song;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.response.LikedSongResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.service.song.LikeService;
import com.melodyHub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Song likes. All endpoints require an authenticated user.
 *
 * <pre>
 *   GET    /api/likes            paged list of the user's liked songs
 *   GET    /api/likes/ids        ids of every liked song (frontend hydration)
 *   POST   /api/likes            body {"songId": n} — like a song
 *   DELETE /api/likes/{songId}   unlike a song
 * </pre>
 */
public class LikeServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private LikeService likeService;

    @Override
    public void init() throws ServletException {
        likeService = new LikeService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        String path = getPath(request);
        try {
            if ("/ids".equals(path)) {
                List<Integer> ids = likeService.likedSongIds(userId.get());
                writeJson(response, HttpServletResponse.SC_OK, Map.of("ids", ids));
                return;
            }

            if ("/".equals(path)) {
                int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
                int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
                if (size > MAX_SIZE) {
                    throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
                }
                PagedResponse<LikedSongResponse> payload = likeService.getPage(userId.get(), page, size);
                writeJson(response, HttpServletResponse.SC_OK, payload);
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Like endpoint was not found");
        } catch (InvalidQueryParamException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        LikeRequest payload;
        try {
            payload = objectMapper.readValue(request.getInputStream(), LikeRequest.class);
        } catch (IOException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON");
            return;
        }

        if (payload == null || payload.songId() == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYLOAD", "songId is required");
            return;
        }

        try {
            likeService.like(userId.get(), payload.songId());
            writeJson(response, HttpServletResponse.SC_OK, Map.of("liked", true));
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        String path = getPath(request);
        try {
            Integer songId = getSongId(path);
            if (songId == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Like endpoint was not found");
                return;
            }
            likeService.unlike(userId.get(), songId);
            writeJson(response, HttpServletResponse.SC_OK, Map.of("liked", false));
        } catch (InvalidQueryParamException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    private Integer getSongId(String path) throws InvalidQueryParamException {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty() || trimmed.contains("/")) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            throw new InvalidQueryParamException("song id must be a positive integer");
        }
    }

    private Optional<Integer> requireUserId(HttpServletRequest request) {
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

    /** Like request body: {"songId": n}. */
    private record LikeRequest(Integer songId) {}
}
