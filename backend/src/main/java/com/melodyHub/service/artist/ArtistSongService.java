package com.melodyHub.service.artist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyHub.dto.request.SongCreateRequest;
import com.melodyHub.dto.request.SongUpdateRequest;
import com.melodyHub.dto.request.SyncedLyricsRequest;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.exception.SongException;
import com.melodyHub.repository.SongLyricsRepository;
import com.melodyHub.repository.SongRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ArtistSongService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SLUG_LENGTH = 280;
    private static final int MAX_URL_LENGTH = 500;
    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final SongRepository songRepository;
    private final SongLyricsRepository lyricsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArtistSongService() {
        this(new SongRepository(), new SongLyricsRepository());
    }

    public ArtistSongService(SongRepository songRepository, SongLyricsRepository lyricsRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
        this.lyricsRepository = Objects.requireNonNull(lyricsRepository, "lyricsRepository must not be null");
    }

    public SongResponse createOwnSong(int artistId, SongCreateRequest request)
            throws SongException, SQLException {
        validateCreateRequest(request);

        Song song = new Song();
        song.setTitle(request.getTitle().trim());
        song.setSlug(request.getSlug().trim());
        song.setFilePath(request.getAudioUrl().trim());
        song.setCoverUrl(normalizeOptional(request.getCoverUrl()));
        song.setDurationSec(request.getDurationSec() == null ? 0 : Math.max(0, request.getDurationSec()));
        song.setLyrics(normalizeOptional(request.getLyrics()));
        song.setLyricsType(parseLyricsType(request.getLyricsType()));
        song.setStatus(SongStatus.PUBLISHED);

        try {
            Song created = songRepository.create(song, artistId);
            // Persist synced lyric lines into song_lyrics so the lyrics API can serve them.
            if (created.getLyricsType() == LyricsType.SYNCED) {
                persistSyncedLyrics(created.getId(), request.getLyrics());
            }
            return SongResponse.fromEntity(created);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE) {
                throw new SongException("SONG_SLUG_EXISTS", "A song with this slug already exists");
            }
            throw exception;
        }
    }

    /**
     * Parses the synced-lyrics JSON ({"lines":[{startTime,endTime,text}],...})
     * and stores the lines in the song_lyrics table (replacing any existing).
     */
    private void persistSyncedLyrics(int songId, String lyricsJson) throws SQLException, SongException {
        lyricsRepository.replaceForSong(songId, parseSyncedLyrics(lyricsJson));
    }

    private List<SongLyricsRepository.SyncedLyricLine> parseSyncedLyrics(String lyricsJson)
            throws SongException {
        if (lyricsJson == null || lyricsJson.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(lyricsJson);
            JsonNode lyricLines = root.get("lines");
            if (!root.isObject() || lyricLines == null || !lyricLines.isArray()) {
                throw new SongException("INVALID_SYNCED_LYRICS", "Synced lyrics must contain a lines array");
            }

            List<SongLyricsRepository.SyncedLyricLine> lines = new ArrayList<>();
            for (JsonNode node : lyricLines) {
                String text = node.path("text").asText("").trim();
                if (text.isEmpty()) {
                    continue;
                }

                JsonNode startTimeNode = node.get("startTime");
                if (startTimeNode == null || !startTimeNode.isNumber()) {
                    throw new SongException("INVALID_SYNCED_LYRICS", "Each lyric line needs a numeric start time");
                }
                double startTime = startTimeNode.asDouble();
                if (!Double.isFinite(startTime) || startTime < 0) {
                    throw new SongException("INVALID_SYNCED_LYRICS", "Lyric start times must be zero or greater");
                }

                lines.add(new SongLyricsRepository.SyncedLyricLine(
                        Math.round(startTime * 1000),
                        text
                ));
            }
            lines.sort(Comparator.comparingLong(SongLyricsRepository.SyncedLyricLine::startTimeMs));
            return lines;
        } catch (JsonProcessingException exception) {
            throw new SongException("INVALID_SYNCED_LYRICS", "Synced lyrics must be valid JSON");
        }
    }

    public SongResponse updateOwnSong(int artistId, int songId, SongUpdateRequest request)
            throws SongException, SQLException {
        if (request == null) {
            throw new SongException("INVALID_REQUEST", "Request body is required");
        }

        String title = request.getTitle();
        if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new SongException("INVALID_SONG_TITLE", "Title is required and must be 255 characters or less");
        }

        String coverUrl = normalizeOptional(request.getCoverUrl());
        if (coverUrl != null && (coverUrl.length() > MAX_URL_LENGTH || !isHttpUrl(coverUrl))) {
            throw new SongException("INVALID_COVER_URL", "Cover URL must be a valid HTTP/HTTPS URL");
        }

        String lyrics = normalizeOptional(request.getLyrics());
        String lyricsType = normalizeOptional(request.getLyricsType());
        if (parseLyricsType(lyricsType) == LyricsType.SYNCED) {
            parseSyncedLyrics(lyrics);
        }

        Song updated = songRepository.updateOwn(artistId, songId, title.trim(), coverUrl, lyrics, lyricsType)
                .orElseThrow(() -> new SongException("SONG_NOT_FOUND", "Song was not found"));

        // Keep song_lyrics in sync with the chosen mode.
        if (parseLyricsType(lyricsType) == LyricsType.SYNCED) {
            persistSyncedLyrics(songId, lyrics);
        } else {
            lyricsRepository.deleteBySongId(songId);
        }

        return SongResponse.fromEntity(updated);
    }

    private void validateCreateRequest(SongCreateRequest request) throws SongException {
        if (request == null) {
            throw new SongException("INVALID_REQUEST", "Request body is required");
        }

        String title = request.getTitle();
        if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new SongException("INVALID_SONG_TITLE", "Title is required and must be 255 characters or less");
        }

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            throw new SongException("INVALID_SONG_SLUG", "Slug is required");
        }
        String normalizedSlug = slug.trim();
        if (normalizedSlug.length() > MAX_SLUG_LENGTH || !SLUG_PATTERN.matcher(normalizedSlug).matches()) {
            throw new SongException(
                    "INVALID_SONG_SLUG",
                    "Slug must be lowercase letters, numbers, and hyphens only (e.g. my-song)"
            );
        }

        String audioUrl = request.getAudioUrl();
        if (audioUrl == null || audioUrl.isBlank()) {
            throw new SongException("INVALID_AUDIO_URL", "An uploaded audio file is required");
        }
        if (audioUrl.trim().length() > MAX_URL_LENGTH || !isHttpUrl(audioUrl.trim())) {
            throw new SongException("INVALID_AUDIO_URL", "Audio URL must be a valid HTTP/HTTPS URL");
        }

        String coverUrl = normalizeOptional(request.getCoverUrl());
        if (coverUrl != null && (coverUrl.length() > MAX_URL_LENGTH || !isHttpUrl(coverUrl))) {
            throw new SongException("INVALID_COVER_URL", "Cover URL must be a valid HTTP/HTTPS URL");
        }

        if (request.getDurationSec() != null && request.getDurationSec() < 0) {
            throw new SongException("INVALID_DURATION", "Duration must be zero or a positive number of seconds");
        }

        String lyricsType = request.getLyricsType();
        if (lyricsType != null && !lyricsType.isBlank()) {
            try {
                LyricsType.valueOf(lyricsType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new SongException("INVALID_LYRICS_TYPE", "Lyrics type must be PLAIN or SYNCED");
            }
        }
        if (parseLyricsType(lyricsType) == LyricsType.SYNCED) {
            parseSyncedLyrics(request.getLyrics());
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LyricsType parseLyricsType(String value) {
        if (value == null || value.isBlank()) {
            return LyricsType.PLAIN;
        }
        try {
            return LyricsType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LyricsType.PLAIN;
        }
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    public PagedResponse<SongResponse> getOwnPage(int artistId, int page, int size) throws SQLException {
        int offset = calculateOffset(page, size);
        List<SongResponse> items = songRepository.getOwnedPage(artistId, size, offset)
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
        long total = songRepository.countOwned(artistId);

        return new PagedResponse<>(items, total, page, size);
    }

    private int calculateOffset(int page, int size) {
        long offset = ((long) page - 1L) * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page is too large");
        }
        return (int) offset;
    }

    public Optional<SongResponse> getOwnByIdentifier(int artistId, String identifier) throws SQLException {
        String normalizedIdentifier = normalize(identifier);
        if (normalizedIdentifier == null) {
            return Optional.empty();
        }

        Integer songId = parsePositiveInteger(normalizedIdentifier);
        if (songId != null) {
            Optional<SongResponse> songById = songRepository.findOwnedById(artistId, songId)
                    .map(SongResponse::fromEntity);
            if (songById.isPresent()) {
                return songById;
            }
        }

        return songRepository.findOwnedBySlug(artistId, normalizedIdentifier)
                .map(SongResponse::fromEntity);
    }
    
    /**
     * Updates synced lyrics for a song. Replaces all existing lyric lines.
     */
    public void updateSyncedLyrics(int artistId, int songId, SyncedLyricsRequest request)
            throws SongException, SQLException {
        // Verify ownership
        Optional<Song> song = songRepository.findOwnedById(artistId, songId);
        if (song.isEmpty()) {
            throw new SongException("SONG_NOT_FOUND", "Song was not found");
        }
        
        // Parse and save synced lyrics
        List<SongLyricsRepository.SyncedLyricLine> lines = new ArrayList<>();
        if (request != null && request.getLines() != null) {
            for (SyncedLyricsRequest.LyricLine line : request.getLines()) {
                if (line == null) {
                    continue;
                }
                if (line.text() != null && !line.text().isBlank()) {
                    if (!Double.isFinite(line.startTime()) || line.startTime() < 0) {
                        throw new SongException(
                                "INVALID_SYNCED_LYRICS",
                                "Lyric start times must be zero or greater"
                        );
                    }
                    lines.add(new SongLyricsRepository.SyncedLyricLine(
                            Math.round(line.startTime() * 1000),  // Convert to ms
                            line.text().trim()
                    ));
                }
            }
        }
        lines.sort(Comparator.comparingLong(SongLyricsRepository.SyncedLyricLine::startTimeMs));
        lyricsRepository.replaceForSong(songId, lines);
    }

    private Integer parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
