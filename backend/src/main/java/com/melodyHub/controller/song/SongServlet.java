package com.melodyHub.controller.song;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.response.SongDetailResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.dto.response.SyncedLyricsResponse;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.repository.SongLyricsRepository;
import com.melodyHub.service.song.SongService;
import com.melodyHub.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SongServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_RELATED_SIZE = 8;
    private static final int MAX_RELATED_SIZE = 12;

    private SongService songService;
    private SongLyricsRepository lyricsRepository;

    @Override
    public void init() throws ServletException {
        songService = new SongService();
        lyricsRepository = new SongLyricsRepository();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                handleGetPage(request, response);
                return;
            }

            String lyricsSegment = getLyricsSegment(path);
            if (lyricsSegment != null) {
                handleGetLyrics(request, response, lyricsSegment);
                return;
            }

            String relatedSegment = getRelatedSegment(path);
            if (relatedSegment != null) {
                handleGetRelated(request, response, relatedSegment);
                return;
            }

            String slug = getSlug(path);
            if (slug != null) {
                handleGetBySlug(request, response, slug);
                return;
            }

            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "Song endpoint was not found"
            );
        } catch (InvalidQueryParamException exception) {
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
    
    private void handleGetLyrics(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException {
        // Reading lyrics must NOT count as a play, so use the read-only lookup
        // instead of getDetail (which bumps play_count).
        Optional<SongResponse> songOpt = songService.getBySlug(slug);

        if (songOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "SONG_NOT_FOUND", "Song was not found");
            return;
        }

        SongResponse song = songOpt.get();

        // If plain lyrics, return empty
        if (song.getLyricsType() != LyricsType.SYNCED) {
            writeJson(response, HttpServletResponse.SC_OK, new SyncedLyricsResponse(song.getId(), "PLAIN", List.of()));
            return;
        }

        // Get synced lyrics from song_lyrics table
        List<SongLyricsRepository.SyncedLyricLine> dbLines = lyricsRepository.findBySongId(song.getId());
        Map<Long, Double> configuredEndTimes = configuredEndTimes(song.getLyrics());
        
        // Convert to response format
        List<SyncedLyricsResponse.LyricLine> lines = new ArrayList<>();
        for (int i = 0; i < dbLines.size(); i++) {
            SongLyricsRepository.SyncedLyricLine dbLine = dbLines.get(i);
            double startTime = dbLine.startTimeMs() / 1000.0;
            double configuredEnd = configuredEndTimes.getOrDefault(dbLine.startTimeMs(), 0.0);
            double nextStart = i < dbLines.size() - 1
                    ? dbLines.get(i + 1).startTimeMs() / 1000.0
                    : startTime + 3.5;
            double endTime = configuredEnd > startTime ? configuredEnd : nextStart;
            
            lines.add(new SyncedLyricsResponse.LyricLine(startTime, endTime, dbLine.text()));
        }
        
        writeJson(response, HttpServletResponse.SC_OK, new SyncedLyricsResponse(song.getId(), "SYNCED", lines));
    }

    /** Reads the explicit End values kept in the synced-lyrics JSON payload. */
    private Map<Long, Double> configuredEndTimes(String lyricsJson) {
        Map<Long, Double> result = new HashMap<>();
        if (lyricsJson == null || lyricsJson.isBlank()) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(lyricsJson);
            JsonNode lyricLines = root.path("lines");
            if (!lyricLines.isArray()) {
                return result;
            }
            for (JsonNode line : lyricLines) {
                if (!line.has("startTime") || !line.has("endTime")) {
                    continue;
                }
                double start = line.get("startTime").asDouble(Double.NaN);
                double end = line.get("endTime").asDouble(Double.NaN);
                if (Double.isFinite(start) && Double.isFinite(end) && end > start) {
                    result.put(Math.round(start * 1000), end);
                }
            }
        } catch (Exception ignored) {
            // The database timestamps remain a safe fallback for legacy data.
        }
        return result;
    }
    
    private String getLyricsSegment(String path) {
        String suffix = "/lyrics";
        if (!path.endsWith(suffix)) {
            return null;
        }
        String slug = path.substring(1, path.length() - suffix.length());
        if (slug.isBlank() || slug.contains("/")) {
            return null;
        }
        return slug;
    }

    private void handleGetPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        String titleQuery = request.getParameter("q");
        String genreSlug = request.getParameter("genre");
        writeJson(response, HttpServletResponse.SC_OK, songService.getPage(page, size, titleQuery, genreSlug));
    }

    private void handleGetBySlug(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException {
        Optional<SongDetailResponse> detail = songService.getDetail(slug, currentUserId(request).orElse(null));
        if (detail.isPresent()) {
            writeJson(response, HttpServletResponse.SC_OK, detail.get());
            return;
        }

        writeError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "SONG_NOT_FOUND",
                "Song was not found"
        );
    }

    private void handleGetRelated(HttpServletRequest request, HttpServletResponse response, String slug)
            throws IOException, SQLException, InvalidQueryParamException {
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_RELATED_SIZE);
        if (size > MAX_RELATED_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_RELATED_SIZE);
        }

        List<SongSummaryResponse> related = songService.getRelated(slug, size);
        if (related == null) {
            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "SONG_NOT_FOUND",
                    "Song was not found"
            );
            return;
        }
        writeJson(response, HttpServletResponse.SC_OK, Map.of("items", related));
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

    private String getSlug(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty() || trimmed.contains("/")) {
            return null;
        }

        return trimmed;
    }

    private String getRelatedSegment(String path) {
        String suffix = "/related";
        if (!path.endsWith(suffix)) {
            return null;
        }
        String slug = path.substring(1, path.length() - suffix.length());
        if (slug.isBlank() || slug.contains("/")) {
            return null;
        }
        return slug;
    }
}
