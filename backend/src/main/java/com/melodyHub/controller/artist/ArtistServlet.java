package com.melodyHub.controller.artist;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ArtistProfileUpdateRequest;
import com.melodyHub.dto.request.BecomeArtistRequest;
import com.melodyHub.dto.request.SongCreateRequest;
import com.melodyHub.dto.request.SongUpdateRequest;
import com.melodyHub.dto.request.SyncedLyricsRequest;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.exception.SongException;
import com.melodyHub.service.artist.ArtistAccountService;
import com.melodyHub.service.artist.ArtistRegistrationService;
import com.melodyHub.service.artist.ArtistSongService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

public class ArtistServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private ArtistAccountService artistAccountService;
    private ArtistSongService artistSongService;
    private ArtistRegistrationService artistRegistrationService;

    @Override
    public void init() throws ServletException {
        artistAccountService = new ArtistAccountService();
        artistSongService = new ArtistSongService();
        artistRegistrationService = new ArtistRegistrationService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/request".equals(path)) {
                // Any authenticated user can check their own artist request status.
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        artistRegistrationService.getMyRequest(getBearerToken(request))
                );
                return;
            }

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            String path = getPath(request);
            if ("/become".equals(path)) {
                BecomeArtistRequest becomeRequest = objectMapper.readValue(
                        request.getInputStream(),
                        BecomeArtistRequest.class
                );
                writeJson(
                        response,
                        HttpServletResponse.SC_CREATED,
                        artistRegistrationService.submitRequest(getBearerToken(request), becomeRequest)
                );
                return;
            }

            if ("/songs".equals(path) || "/songs/".equals(path)) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                SongCreateRequest songRequest = objectMapper.readValue(
                        request.getInputStream(),
                        SongCreateRequest.class
                );
                writeJson(
                        response,
                        HttpServletResponse.SC_CREATED,
                        artistSongService.createOwnSong(currentArtist.getId(), songRequest)
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
        } catch (SongException exception) {
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
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            String path = getPath(request);
            if ("/profile".equals(path)) {
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

            Integer songId = parseSongId(path);
            if (songId != null) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                SongUpdateRequest updateRequest = objectMapper.readValue(
                        request.getInputStream(),
                        SongUpdateRequest.class
                );
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        artistSongService.updateOwnSong(currentArtist.getId(), songId, updateRequest)
                );
                return;
            }
            
            Integer lyricsSongId = parseSongLyricsId(path);
            if (lyricsSongId != null) {
                Artist currentArtist = artistAccountService.getCurrentArtist(getBearerToken(request));
                SyncedLyricsRequest lyricsRequest = objectMapper.readValue(
                        request.getInputStream(),
                        SyncedLyricsRequest.class
                );
                artistSongService.updateSyncedLyrics(currentArtist.getId(), lyricsSongId, lyricsRequest);
                writeJson(response, HttpServletResponse.SC_OK, Map.of("success", true));
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
        } catch (SongException exception) {
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
    
    private Integer parseSongLyricsId(String path) {
        String prefix = "/songs/";
        String suffix = "/lyrics";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }
        String identifier = path.substring(prefix.length(), path.length() - suffix.length());
        if (identifier.isBlank() || identifier.contains("/")) {
            return null;
        }
        try {
            int id = Integer.parseInt(identifier);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseSongId(String path) {
        String identifier = getSongIdentifier(path);
        if (identifier == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(identifier);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED", "FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND", "ARTIST_PROFILE_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private int getStatusCode(SongException exception) {
        return switch (exception.getCode()) {
            case "SONG_SLUG_EXISTS" -> HttpServletResponse.SC_CONFLICT;
            case "SONG_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private int getStatusCode(ArtistException exception) {
        return switch (exception.getCode()) {
            case "ARTIST_SLUG_EXISTS",
                    "ARTIST_ALREADY_EXISTS",
                    "ARTIST_REQUEST_PENDING_EXISTS",
                    "ARTIST_REQUEST_NOT_PENDING" -> HttpServletResponse.SC_CONFLICT;
            case "ARTIST_REQUEST_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            case "INVALID_ARTIST_NAME",
                    "INVALID_ARTIST_SLUG",
                    "INVALID_ARTIST_BIO",
                    "INVALID_ARTIST_IMAGE_URL",
                    "INVALID_REVIEW_NOTE",
                    "INVALID_REQUEST" -> HttpServletResponse.SC_BAD_REQUEST;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}
