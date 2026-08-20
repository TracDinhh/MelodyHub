package com.melodyHub.controller.studio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ArtistProfileUpdateRequest;
import com.melodyHub.dto.request.SongCreateRequest;
import com.melodyHub.dto.request.SongUpdateRequest;
import com.melodyHub.dto.request.SyncedLyricsRequest;
import com.melodyHub.entity.ArtistMember;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.exception.SongException;
import com.melodyHub.lyrics.LyricsLookupResponse;
import com.melodyHub.lyrics.LyricsLookupService;
import com.melodyHub.service.artist.ArtistAccountService;
import com.melodyHub.service.artist.ArtistAuthorizationService;
import com.melodyHub.service.artist.ArtistSongService;
import com.melodyHub.service.artist.ArtistStatsService;
import com.melodyHub.service.auth.AuthorizationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * Studio management API. Mapped to {@code /api/studio/*}.
 *
 * <p>All routes require an authenticated user who is a member of the artist
 * identified in the URL path ({@code /artists/{artistId}/...}).
 * Access control is enforced by {@link ArtistAuthorizationService} on every request.</p>
 *
 * <h2>Routes</h2>
 * <pre>
 * GET  /api/studio/artists/{artistId}/profile
 * PUT  /api/studio/artists/{artistId}/profile
 * GET  /api/studio/artists/{artistId}/songs
 * GET  /api/studio/artists/{artistId}/songs/{songId}
 * POST /api/studio/artists/{artistId}/songs
 * PUT  /api/studio/artists/{artistId}/songs/{songId}
 * PUT  /api/studio/artists/{artistId}/songs/{songId}/lyrics
 * GET  /api/studio/artists/{artistId}/stats
 * GET  /api/studio/artists/{artistId}/analytics
 * </pre>
 */
public class StudioServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private ArtistAuthorizationService artistAuthorizationService;
    private ArtistAccountService artistAccountService;
    private ArtistSongService artistSongService;
    private ArtistStatsService artistStatsService;
    private LyricsLookupService lyricsLookupService;
    private AuthorizationService authorizationService;

    @Override
    public void init() throws ServletException {
        artistAuthorizationService = new ArtistAuthorizationService();
        artistAccountService = new ArtistAccountService();
        artistSongService = new ArtistSongService();
        artistStatsService = new ArtistStatsService();
        lyricsLookupService = new LyricsLookupService();
        authorizationService = new AuthorizationService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            String token = getBearerToken(request);

            // /lyrics/search — authenticated lyrics lookup (Studio tooling)
            if ("/lyrics/search".equals(path)) {
                authorizationService.requireAuthenticated(token);
                handleLyricsSearch(request, response);
                return;
            }

            // /artists/{artistId}/profile
            ArtistPathResult profilePath = matchArtistPath(path, "profile");
            if (profilePath != null) {
                artistAuthorizationService.requireCanEditArtist(token, profilePath.artistId());
                writeJson(response, HttpServletResponse.SC_OK,
                        artistAccountService.getArtistProfile(profilePath.artistId()));
                return;
            }

            // /artists/{artistId}/songs
            ArtistPathResult songsPath = matchArtistPath(path, "songs");
            if (songsPath != null) {
                artistAuthorizationService.requireCanManageMusic(token, songsPath.artistId());
                int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
                int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
                if (size > MAX_SIZE) throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
                writeJson(response, HttpServletResponse.SC_OK,
                        artistSongService.getSongs(songsPath.artistId(), page, size));
                return;
            }

            // /artists/{artistId}/songs/{songId}
            ArtistSongSubPath songPath = matchSongSubPath(path, null);
            if (songPath != null) {
                artistAuthorizationService.requireCanManageMusic(token, songPath.artistId());
                var song = artistSongService.getOwnSongById(songPath.artistId(), songPath.songId());
                writeJson(response, HttpServletResponse.SC_OK, com.melodyHub.dto.response.SongResponse.fromEntity(song));
                return;
            }

            // /artists/{artistId}/stats
            ArtistPathResult statsPath = matchArtistPath(path, "stats");
            if (statsPath != null) {
                artistAuthorizationService.requireArtistAccess(token, statsPath.artistId());
                writeJson(response, HttpServletResponse.SC_OK,
                        artistStatsService.getStats(statsPath.artistId()));
                return;
            }

            // /artists/{artistId}/analytics
            ArtistPathResult analyticsPath = matchArtistPath(path, "analytics");
            if (analyticsPath != null) {
                artistAuthorizationService.requireArtistAccess(token, analyticsPath.artistId());
                writeJson(response, HttpServletResponse.SC_OK,
                        artistStatsService.getAnalytics(analyticsPath.artistId()));
                return;
            }

            // /artists/{artistId}/songs/{songId}/lyrics/lookup
            ArtistSongSubPath lyricsLookupPath = matchSongSubPath(path, "lyrics/lookup");
            if (lyricsLookupPath != null) {
                artistAuthorizationService.requireCanManageMusic(token, lyricsLookupPath.artistId());
                var song = artistSongService.getOwnSongById(lyricsLookupPath.artistId(), lyricsLookupPath.songId());
                writeJson(response, HttpServletResponse.SC_OK,
                        lyricsLookupService.lookup(song.getTitle(), null, null, null));
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Studio endpoint not found");
        } catch (InvalidQueryParamException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", e.getMessage());
        } catch (AuthException e) {
            int status = "ARTIST_ACCESS_DENIED".equals(e.getCode()) || "ARTIST_OWNER_REQUIRED".equals(e.getCode())
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED;
            writeError(response, status, e.getCode(), e.getMessage());
        } catch (ArtistException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
        } catch (SongException e) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, e.getCode(), e.getMessage());
        } catch (SQLException e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            String token = getBearerToken(request);

            // POST /artists/{artistId}/songs
            ArtistPathResult songsPath = matchArtistPath(path, "songs");
            if (songsPath != null) {
                ArtistMember member = artistAuthorizationService.requireCanManageMusic(token, songsPath.artistId());
                SongCreateRequest body = readBody(request, SongCreateRequest.class);
                writeJson(response, HttpServletResponse.SC_CREATED,
                        artistSongService.createSong(songsPath.artistId(), body));
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Studio endpoint not found");
        } catch (JsonProcessingException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Malformed JSON body");
        } catch (AuthException e) {
            int status = "ARTIST_ACCESS_DENIED".equals(e.getCode()) ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED;
            writeError(response, status, e.getCode(), e.getMessage());
        } catch (SongException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
        } catch (SQLException e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            String token = getBearerToken(request);

            // PUT /artists/{artistId}/profile
            ArtistPathResult profilePath = matchArtistPath(path, "profile");
            if (profilePath != null) {
                artistAuthorizationService.requireCanEditArtist(token, profilePath.artistId());
                ArtistProfileUpdateRequest body = readBody(request, ArtistProfileUpdateRequest.class);
                var artist = artistAccountService.getArtistEntity(profilePath.artistId());
                writeJson(response, HttpServletResponse.SC_OK,
                        artistAccountService.updateCurrentArtistProfile(artist, body));
                return;
            }

            // PUT /artists/{artistId}/songs/{songId}
            ArtistSongSubPath songPath = matchSongSubPath(path, null);
            if (songPath != null && songPath.subPath() == null) {
                artistAuthorizationService.requireCanManageMusic(token, songPath.artistId());
                SongUpdateRequest body = readBody(request, SongUpdateRequest.class);
                writeJson(response, HttpServletResponse.SC_OK,
                        artistSongService.updateOwnSong(songPath.artistId(), songPath.songId(), body));
                return;
            }

            // PUT /artists/{artistId}/songs/{songId}/lyrics
            ArtistSongSubPath lyricsPath = matchSongSubPath(path, "lyrics");
            if (lyricsPath != null) {
                artistAuthorizationService.requireCanManageMusic(token, lyricsPath.artistId());
                SyncedLyricsRequest body = readBody(request, SyncedLyricsRequest.class);
                artistSongService.updateOwnSongSyncedLyrics(lyricsPath.artistId(), lyricsPath.songId(), body);
                writeJson(response, HttpServletResponse.SC_OK, Map.of("success", true));
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Studio endpoint not found");
        } catch (JsonProcessingException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Malformed JSON body");
        } catch (AuthException e) {
            int status = "ARTIST_ACCESS_DENIED".equals(e.getCode()) ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED;
            writeError(response, status, e.getCode(), e.getMessage());
        } catch (ArtistException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
        } catch (SongException e) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, e.getCode(), e.getMessage());
        } catch (SQLException e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
        }
    }

    private void handleLyricsSearch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String title = request.getParameter("title");
        String artist = request.getParameter("artist");
        String album = request.getParameter("album");
        String durationParam = request.getParameter("duration");

        Integer duration = null;
        if (durationParam != null && !durationParam.isBlank()) {
            try {
                duration = Integer.parseInt(durationParam.trim());
            } catch (NumberFormatException e) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "INVALID_LYRICS_SEARCH", "Duration must be a number");
                return;
            }
        }

        try {
            LyricsLookupResponse result = lyricsLookupService.lookup(
                    title, artist, album, duration);
            writeJson(response, HttpServletResponse.SC_OK, result);
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_LYRICS_SEARCH", e.getMessage());
        }
    }

    // ─── Path matching helpers ────────────────────────────────────────────────

    /**
     * Matches paths of the form {@code /artists/{artistId}/{sub}}.
     * Returns null if the path does not match.
     */
    private ArtistPathResult matchArtistPath(String path, String sub) {
        // /artists/{artistId}/{sub}
        String prefix = "/artists/";
        if (!path.startsWith(prefix)) return null;

        String rest = path.substring(prefix.length()); // "{artistId}/{sub}"
        String suffix = "/" + sub;
        if (!rest.endsWith(suffix)) return null;

        String idStr = rest.substring(0, rest.length() - suffix.length());
        if (idStr.contains("/")) return null;

        try {
            return new ArtistPathResult(Integer.parseInt(idStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Matches paths of the form {@code /artists/{artistId}/songs/{songId}} or
     * {@code /artists/{artistId}/songs/{songId}/{subPath}}.
     */
    private ArtistSongSubPath matchSongSubPath(String path, String subPath) {
        // /artists/{artistId}/songs/{songId}[/{subPath}]
        String prefix = "/artists/";
        if (!path.startsWith(prefix)) return null;

        String rest = path.substring(prefix.length());
        String[] segments = rest.split("/");
        // segments[0] = artistId, segments[1] = "songs", segments[2] = songId, segments[3+] = subPath

        if (segments.length < 3 || !"songs".equals(segments[1])) return null;

        int artistId, songId;
        try {
            artistId = Integer.parseInt(segments[0]);
            songId = Integer.parseInt(segments[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (subPath == null) {
            // Must be exact: /artists/{artistId}/songs/{songId}
            return segments.length == 3 ? new ArtistSongSubPath(artistId, songId, null) : null;
        } else {
            // Must end with the subPath
            String suffix = "/" + subPath;
            return rest.endsWith(suffix) ? new ArtistSongSubPath(artistId, songId, subPath) : null;
        }
    }

    private <T> T readBody(HttpServletRequest request, Class<T> type) throws IOException {
        String charset = request.getCharacterEncoding();
        if (charset == null) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        return objectMapper.readValue(request.getInputStream(), type);
    }

    private record ArtistPathResult(int artistId) {}
    private record ArtistSongSubPath(int artistId, int songId, String subPath) {}
}
