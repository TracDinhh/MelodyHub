package com.melodyHub.controller.album;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.AlbumCreateRequest;
import com.melodyHub.dto.request.AlbumUpdateRequest;
import com.melodyHub.dto.response.AlbumResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.exception.AlbumException;
import com.melodyHub.service.album.AlbumService;
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
 * Album management for artists.
 *
 * <ul>
 *   <li>{@code GET    /api/studio/albums}                     — page of artist's albums</li>
 *   <li>{@code POST   /api/studio/albums}                     — create an album</li>
 *   <li>{@code GET    /api/studio/albums/{id}}               — get album detail</li>
 *   <li>{@code PUT    /api/studio/albums/{id}}               — update an album</li>
 *   <li>{@code DELETE /api/studio/albums/{id}}               — delete an album</li>
 *   <li>{@code GET    /api/studio/albums/{id}/songs}         — get songs in album</li>
 * </ul>
 */
public class AlbumServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private AlbumService albumService;

    @Override
    public void init() throws ServletException {
        albumService = new AlbumService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        // Get artist's artistId from query param
        String artistIdParam = request.getParameter("artistId");
        if (artistIdParam == null || artistIdParam.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_ARTIST_ID", "artistId query param is required");
            return;
        }

        int artistId;
        try {
            artistId = Integer.parseInt(artistIdParam);
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_ARTIST_ID", "artistId must be a number");
            return;
        }

        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleList(request, response, artistId);
                return;
            }

            Integer albumId = idAt(path, 0);
            if (albumId != null && segmentCount(path) == 1) {
                handleDetail(response, artistId, albumId);
                return;
            }

            if (albumId != null && segmentCount(path) == 2 && "songs".equals(segment(path, 1))) {
                handleSongs(response, artistId, albumId);
                return;
            }

            writeNotFound(response);
        } catch (IllegalArgumentException exception) {
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

        String artistIdParam = request.getParameter("artistId");
        if (artistIdParam == null || artistIdParam.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_ARTIST_ID", "artistId query param is required");
            return;
        }

        int artistId;
        try {
            artistId = Integer.parseInt(artistIdParam);
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_ARTIST_ID", "artistId must be a number");
            return;
        }

        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleCreate(request, response, artistId);
                return;
            }

            writeNotFound(response);
        } catch (AlbumException exception) {
            writeAlbumError(response, exception);
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
            Integer albumId = idAt(path, 0);
            if (albumId == null || segmentCount(path) != 1) {
                writeNotFound(response);
                return;
            }

            // Get artistId from query param
            String artistIdParam = request.getParameter("artistId");
            if (artistIdParam == null || artistIdParam.isBlank()) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_ARTIST_ID", "artistId query param is required");
                return;
            }

            int artistId = Integer.parseInt(artistIdParam);
            handleUpdate(request, response, artistId, albumId);
        } catch (AlbumException exception) {
            writeAlbumError(response, exception);
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
            Integer albumId = idAt(path, 0);
            if (albumId == null || segmentCount(path) != 1) {
                writeNotFound(response);
                return;
            }

            String artistIdParam = request.getParameter("artistId");
            if (artistIdParam == null || artistIdParam.isBlank()) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_ARTIST_ID", "artistId query param is required");
                return;
            }

            int artistId = Integer.parseInt(artistIdParam);
            handleDelete(response, artistId, albumId);
        } catch (AlbumException exception) {
            writeAlbumError(response, exception);
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    // ---- Handlers -------------------------------------------------------

    private void handleList(HttpServletRequest request, HttpServletResponse response, int artistId)
            throws IOException, SQLException {
        int page;
        int size;
        try {
            page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
            size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        } catch (InvalidQueryParamException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", e.getMessage());
            return;
        }
        if (size > MAX_SIZE) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", "size must not exceed " + MAX_SIZE);
            return;
        }
        PagedResponse<AlbumResponse> payload = albumService.getPage(artistId, page, size);
        writeJson(response, HttpServletResponse.SC_OK, payload);
    }

    private void handleDetail(HttpServletResponse response, int artistId, int albumId)
            throws IOException, SQLException {
        Optional<AlbumResponse> album = albumService.getById(artistId, albumId);
        if (album.isEmpty()) {
            writeAlbumNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, album.get());
    }

    private void handleSongs(HttpServletResponse response, int artistId, int albumId)
            throws IOException, SQLException {
        Optional<AlbumResponse> album = albumService.getById(artistId, albumId);
        if (album.isEmpty()) {
            writeAlbumNotFound(response);
            return;
        }
        try {
            List<Map<String, Object>> songs = albumService.getSongsInAlbum(artistId, albumId);
            writeJson(response, HttpServletResponse.SC_OK, songs);
        } catch (AlbumException e) {
            writeAlbumError(response, e);
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response, int artistId)
            throws IOException, AlbumException, SQLException {
        AlbumCreateRequest payload = readBody(request, AlbumCreateRequest.class);
        if (payload == null) {
            writeInvalidJson(response);
            return;
        }
        AlbumResponse created = albumService.create(artistId, payload);
        writeJson(response, HttpServletResponse.SC_CREATED, created);
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, int artistId, int albumId)
            throws IOException, AlbumException, SQLException {
        AlbumUpdateRequest payload = readBody(request, AlbumUpdateRequest.class);
        if (payload == null) {
            writeInvalidJson(response);
            return;
        }
        AlbumResponse updated = albumService.update(artistId, albumId, payload);
        writeJson(response, HttpServletResponse.SC_OK, updated);
    }

    private void handleDelete(HttpServletResponse response, int artistId, int albumId)
            throws IOException, AlbumException, SQLException {
        if (!albumService.delete(artistId, albumId)) {
            writeAlbumNotFound(response);
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("deleted", true));
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
        String[] parts = segments(path);
        if (index >= parts.length || parts[index] == null || parts[index].isEmpty()) {
            return null;
        }
        try {
            int id = Integer.parseInt(parts[index]);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void writeNotFound(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Album endpoint was not found");
    }

    private void writeAlbumNotFound(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "ALBUM_NOT_FOUND", "Album was not found");
    }

    private void writeAlbumError(HttpServletResponse response, AlbumException exception) throws IOException {
        String code = exception.getCode();
        int status = switch (code) {
            case "ALBUM_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            case "ALBUM_TITLE_REQUIRED", "ALBUM_TITLE_TOO_LONG", "INVALID_ALBUM_TYPE",
                 "INVALID_COVER_URL", "INVALID_REQUEST" -> HttpServletResponse.SC_BAD_REQUEST;
            case "ALBUM_SLUG_EXISTS" -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeError(response, status, code, exception.getMessage());
    }

    private void writeInvalidJson(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON");
    }

    private void writeDatabaseError(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
    }
}
