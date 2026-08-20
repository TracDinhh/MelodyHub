package com.melodyHub.controller.artist;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.service.artist.ArtistFollowService;
import com.melodyHub.service.artist.ArtistFollowService.FollowState;
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
 * Artist follows. All endpoints require an authenticated user.
 *
 * <pre>
 *   GET    /api/follows            paged list of the user's followed artists
 *   GET    /api/follows/ids        ids of every followed artist (frontend hydration)
 *   POST   /api/follows            body {"artistId": n} — follow an artist
 *   DELETE /api/follows/{artistId} unfollow an artist
 * </pre>
 */
public class ArtistFollowServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private ArtistFollowService artistFollowService;

    @Override
    public void init() throws ServletException {
        artistFollowService = new ArtistFollowService();
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
                List<Integer> ids = artistFollowService.followingIds(userId.get());
                writeJson(response, HttpServletResponse.SC_OK, Map.of("ids", ids));
                return;
            }

            if ("/".equals(path)) {
                int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
                int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
                if (size > MAX_SIZE) {
                    throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
                }
                PagedResponse<?> payload = artistFollowService.getFollowingPage(userId.get(), page, size);
                writeJson(response, HttpServletResponse.SC_OK, payload);
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Follow endpoint was not found");
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

        FollowRequest payload;
        try {
            payload = objectMapper.readValue(request.getInputStream(), FollowRequest.class);
        } catch (IOException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON");
            return;
        }

        if (payload == null || payload.artistId() == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYLOAD", "artistId is required");
            return;
        }

        try {
            FollowState state = artistFollowService.follow(userId.get(), payload.artistId());
            writeJson(response, HttpServletResponse.SC_OK, toPayload(state));
        } catch (ArtistException exception) {
            writeArtistError(response, exception);
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
            Integer artistId = getArtistId(path);
            if (artistId == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Follow endpoint was not found");
                return;
            }
            FollowState state = artistFollowService.unfollow(userId.get(), artistId);
            writeJson(response, HttpServletResponse.SC_OK, toPayload(state));
        } catch (ArtistException exception) {
            writeArtistError(response, exception);
        } catch (InvalidQueryParamException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    private void writeArtistError(HttpServletResponse response, ArtistException exception) throws IOException {
        if ("ARTIST_NOT_FOUND".equals(exception.getCode())) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, exception.getCode(), exception.getMessage());
            return;
        }
        writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    private Map<String, Object> toPayload(FollowState state) {
        return Map.of(
                "following", state.following(),
                "followerCount", state.followerCount()
        );
    }

    private Integer getArtistId(String path) throws InvalidQueryParamException {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty() || trimmed.contains("/")) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            throw new InvalidQueryParamException("artist id must be a positive integer");
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

    /** Follow request body: {"artistId": n}. */
    private record FollowRequest(Integer artistId) {}
}