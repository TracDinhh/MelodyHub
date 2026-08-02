package com.melodyHub.controller.playlist;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.request.PlaylistCreateRequest;
import com.melodyHub.dto.request.PlaylistSongRequest;
import com.melodyHub.dto.request.PlaylistUpdateRequest;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.PlaylistDetailResponse;
import com.melodyHub.dto.response.PlaylistResponse;
import com.melodyHub.service.playlist.PlaylistService;
import com.melodyHub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * User playlists.
 *
 * <ul>
 *   <li>{@code GET    /api/playlists}                     — page of the user's playlists</li>
 *   <li>{@code POST   /api/playlists}                     — create a playlist</li>
 *   <li>{@code GET    /api/playlists/{id}}                — playlist detail with tracks</li>
 *   <li>{@code PUT    /api/playlists/{id}}                — update a playlist</li>
 *   <li>{@code DELETE /api/playlists/{id}}                — delete a playlist</li>
 *   <li>{@code POST   /api/playlists/{id}/songs}          — add a song</li>
 *   <li>{@code DELETE /api/playlists/{id}/songs/{songId}} — remove a song</li>
 * </ul>
 */
public class PlaylistServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private PlaylistService playlistService;

    @Override
    public void init() throws ServletException {
        playlistService = new PlaylistService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleList(request, response, userId.get());
                return;
            }

            Integer playlistId = idAt(path, 0);
            if (playlistId != null && segmentCount(path) == 1) {
                handleDetail(response, userId.get(), playlistId);
                return;
            }

            writeNotFound(response);
        } catch (InvalidQueryParamException | IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleCreate(request, response, userId.get());
                return;
            }

            Integer playlistId = idAt(path, 0);
            if (playlistId != null && segmentCount(path) == 2 && "songs".equals(segment(path, 1))) {
                handleAddSong(request, response, userId.get(), playlistId);
                return;
            }

            writeNotFound(response);
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYLOAD", exception.getMessage());
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            String path = getPath(request);
            Integer playlistId = idAt(path, 0);
            if (playlistId == null || segmentCount(path) != 1) {
                writeNotFound(response);
                return;
            }
            handleUpdate(request, response, userId.get(), playlistId);
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYLOAD", exception.getMessage());
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            String path = getPath(request);
            Integer playlistId = idAt(path, 0);
            if (playlistId == null) {
                writeNotFound(response);
                return;
            }

            if (segmentCount(path) == 1) {
                handleDeletePlaylist(response, userId.get(), playlistId);
                return;
            }

            Integer songId = idAt(path, 2);
            if (segmentCount(path) == 3 && "songs".equals(segment(path, 1)) && songId != null) {
                handleRemoveSong(response, userId.get(), playlistId, songId);
                return;
            }

            writeNotFound(response);
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    // ---- Handlers -------------------------------------------------------

    private void handleList(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }
        PagedResponse<PlaylistResponse> payload = playlistService.getPage(userId, page, size);
        writeJson(response, HttpServletResponse.SC_OK, payload);
    }

    private void handleDetail(HttpServletResponse response, int userId, int playlistId)
            throws IOException, SQLException {
        Optional<PlaylistDetailResponse> detail = playlistService.getDetail(userId, playlistId);
        if (detail.isEmpty()) {
            writePlaylistNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, detail.get());
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException, SQLException {
        PlaylistCreateRequest payload = readBody(request, PlaylistCreateRequest.class);
        if (payload == null) {
            writeInvalidJson(response);
            return;
        }
        PlaylistResponse created = playlistService.create(
                userId,
                payload.getName(),
                payload.getDescription(),
                payload.getCoverUrl(),
                payload.getIsPublic()
        );
        writeJson(response, HttpServletResponse.SC_CREATED, created);
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, int userId, int playlistId)
            throws IOException, SQLException {
        PlaylistUpdateRequest payload = readBody(request, PlaylistUpdateRequest.class);
        if (payload == null) {
            writeInvalidJson(response);
            return;
        }
        Optional<PlaylistResponse> updated = playlistService.update(
                userId,
                playlistId,
                payload.getName(),
                payload.getDescription(),
                payload.getCoverUrl(),
                payload.getIsPublic()
        );
        if (updated.isEmpty()) {
            writePlaylistNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, updated.get());
    }

    private void handleDeletePlaylist(HttpServletResponse response, int userId, int playlistId)
            throws IOException, SQLException {
        if (!playlistService.delete(userId, playlistId)) {
            writePlaylistNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("deleted", true));
    }

    private void handleAddSong(HttpServletRequest request, HttpServletResponse response, int userId, int playlistId)
            throws IOException, SQLException {
        PlaylistSongRequest payload = readBody(request, PlaylistSongRequest.class);
        if (payload == null) {
            writeInvalidJson(response);
            return;
        }
        Optional<Boolean> added = playlistService.addSong(userId, playlistId, payload.getSongId());
        if (added.isEmpty()) {
            writePlaylistNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("added", added.get()));
    }

    private void handleRemoveSong(HttpServletResponse response, int userId, int playlistId, int songId)
            throws IOException, SQLException {
        Optional<Boolean> removed = playlistService.removeSong(userId, playlistId, songId);
        if (removed.isEmpty()) {
            writePlaylistNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("removed", removed.get()));
    }

    // ---- Helpers --------------------------------------------------------

    private <T> T readBody(HttpServletRequest request, Class<T> type) throws IOException {
        try {
            return objectMapper.readValue(request.getInputStream(), type);
        } catch (IOException exception) {
            return null;
        }
    }

    private Optional<Integer> requireUserId(HttpServletRequest request) {
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

    private String[] segments(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("/");
    }

    private int segmentCount(String path) {
        return segments(path).length;
    }

    private String segment(String path, int index) {
        String[] parts = segments(path);
        return index < parts.length ? parts[index] : null;
    }

    /** Parses the path segment at {@code index} as a positive int, or null if absent/invalid. */
    private Integer idAt(String path, int index) {
        String value = segment(path, index);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            int id = Integer.parseInt(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    private void writeNotFound(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Playlist endpoint was not found");
    }

    private void writePlaylistNotFound(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "PLAYLIST_NOT_FOUND", "Playlist was not found");
    }

    private void writeInvalidJson(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON");
    }

    private void writeDatabaseError(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
    }

    private static final class InvalidQueryParamException extends Exception {
        private InvalidQueryParamException(String message) {
            super(message);
        }
    }
}
