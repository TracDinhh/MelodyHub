package com.melodyHub.controller.artist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.request.ArtistProfileUpdateRequest;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.artist.ArtistAccountService;
import com.melodyHub.service.artist.ArtistSongService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ArtistServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ArtistAccountService artistAccountService;
    private ArtistSongService artistSongService;

    @Override
    public void init() throws ServletException {
        artistAccountService = new ArtistAccountService();
        artistSongService = new ArtistSongService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/profile".equals(path)) {
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        artistAccountService.getCurrentArtistProfile(getBearerToken(request))
                );
                return;
            }

            if ("/songs".equals(path) || "/songs/".equals(path)) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                handleGetOwnSongPage(request, response, currentArtist);
                return;
            }

            String songIdentifier = getSongIdentifier(path);
            if (songIdentifier != null) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                handleGetOwnSong(response, currentArtist, songIdentifier);
                return;
            }

            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "Artist endpoint was not found"
            );
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (InvalidQueryParamException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_QUERY_PARAM",
                    exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
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

    private void handleGetOwnSongPage(
            HttpServletRequest request,
            HttpServletResponse response,
            Artist currentArtist
    ) throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        writeJson(
                response,
                HttpServletResponse.SC_OK,
                artistSongService.getOwnPage(currentArtist.getId(), page, size)
        );
    }

    private void handleGetOwnSong(
            HttpServletResponse response,
            Artist currentArtist,
            String identifier
    ) throws IOException, SQLException {
        var song = artistSongService.getOwnByIdentifier(currentArtist.getId(), identifier);
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

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            if ("/profile".equals(getPath(request))) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                ArtistProfileUpdateRequest updateRequest = objectMapper.readValue(
                        request.getInputStream(),
                        ArtistProfileUpdateRequest.class
                );
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        artistAccountService.updateCurrentArtistProfile(
                                currentArtist,
                                updateRequest
                        )
                );
                return;
            }

            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "Artist endpoint was not found"
            );
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (ArtistException exception) {
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

    private String getPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
    }

    private String getSongIdentifier(String path) {
        String prefix = "/songs/";
        if (!path.startsWith(prefix)) {
            return null;
        }

        String identifier = path.substring(prefix.length());
        if (identifier.isBlank() || identifier.contains("/")) {
            return null;
        }
        return identifier;
    }

    private int parsePositiveInt(String value, String name, int defaultValue)
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

    private String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED", "FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND", "ARTIST_PROFILE_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private int getStatusCode(ArtistException exception) {
        return switch (exception.getCode()) {
            case "ARTIST_SLUG_EXISTS" -> HttpServletResponse.SC_CONFLICT;
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

    private static final class InvalidQueryParamException extends Exception {
        private InvalidQueryParamException(String message) {
            super(message);
        }
    }
}
