package com.melodyHub.controller.song;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyHub.dto.response.SyncedLyricsResponse;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.repository.SongLyricsRepository;
import com.melodyHub.repository.SongRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns synced lyrics for a song.
 * GET /api/songs/{slug}/lyrics
 *
 * <p>NOTE: This handler is currently unused — {@code SongServlet} already
 * serves {@code /api/songs/{slug}/lyrics} via {@code handleGetLyrics}. The
 * {@code @WebServlet("/api/songs/*")} mapping was removed because it collided
 * with {@code SongServlet}'s mapping in web.xml and prevented the whole web
 * application from starting.
 */
public class SongLyricsServlet extends HttpServlet {
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final SongRepository songRepository = new SongRepository();
    private final SongLyricsRepository lyricsRepository = new SongLyricsRepository();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        
        // Expected: /api/songs/{slug}/lyrics
        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        String[] parts = pathInfo.split("/");
        if (parts.length < 3) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        String slug = parts[1];
        String subPath = parts.length > 2 ? parts[2] : "";
        
        if (!"lyrics".equals(subPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        try {
            Song song = songRepository.findBySlug(slug).orElse(null);
            
            if (song == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // If plain lyrics, just return null
            if (song.getLyricsType() != LyricsType.SYNCED) {
                writeJson(response, new SyncedLyricsResponse(song.getId(), "PLAIN", List.of()));
                return;
            }
            
            // Get synced lyrics from song_lyrics table
            List<SongLyricsRepository.SyncedLyricLine> dbLines = lyricsRepository.findBySongId(song.getId());
            
            // Convert to response format
            List<SyncedLyricsResponse.LyricLine> lines = new ArrayList<>();
            for (int i = 0; i < dbLines.size(); i++) {
                SongLyricsRepository.SyncedLyricLine dbLine = dbLines.get(i);
                double startTime = dbLine.startTimeMs() / 1000.0;
                double endTime;
                
                if (i < dbLines.size() - 1) {
                    endTime = dbLines.get(i + 1).startTimeMs() / 1000.0;
                } else {
                    // Last line - estimate 3.5 seconds or use song duration
                    endTime = startTime + 3.5;
                }
                
                lines.add(new SyncedLyricsResponse.LyricLine(startTime, endTime, dbLine.text()));
            }
            
            writeJson(response, new SyncedLyricsResponse(song.getId(), "SYNCED", lines));
            
        } catch (SQLException e) {
            getServletContext().log("Database error", e);
            writeError(response, "DATABASE_ERROR", "A database error occurred");
        }
    }
    
    private void writeJson(HttpServletResponse response, Object obj) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), obj);
    }
    
    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
    
    private record ErrorResponse(String code, String message) {}
}
