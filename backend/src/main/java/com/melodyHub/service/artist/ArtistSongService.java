package com.melodyHub.service.artist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyHub.dto.request.SongCreateRequest;
import com.melodyHub.dto.request.SongUpdateRequest;
import com.melodyHub.dto.request.SyncedLyricsRequest;
import com.melodyHub.dto.response.GenreResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.Genre;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.exception.SongException;
import com.melodyHub.repository.GenreRepository;
import com.melodyHub.repository.SongLyricsRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ArtistSongService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SLUG_LENGTH = 280;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_GENRES = 3;
    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final SongRepository songRepository;
    private final SongLyricsRepository lyricsRepository;
    private final GenreRepository genreRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArtistSongService() {
        this(new SongRepository(), new SongLyricsRepository(), new GenreRepository());
    }

    public ArtistSongService(SongRepository songRepository, SongLyricsRepository lyricsRepository) {
        this(songRepository, lyricsRepository, new GenreRepository());
    }

    public ArtistSongService(SongRepository songRepository, SongLyricsRepository lyricsRepository,
                             GenreRepository genreRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
        this.lyricsRepository = Objects.requireNonNull(lyricsRepository, "lyricsRepository must not be null");
        this.genreRepository = Objects.requireNonNull(genreRepository, "genreRepository must not be null");
    }

    public SongResponse createSong(int artistId, SongCreateRequest request)
            throws SongException, SQLException {
        validateCreateRequest(request);
        List<Integer> genreIds = validateGenres(request.getGenreIds());

        Song song = new Song();
        song.setTitle(request.getTitle().trim());
        song.setSlug(request.getSlug().trim());
        song.setFilePath(request.getAudioUrl().trim());
        song.setCoverUrl(normalizeOptional(request.getCoverUrl()));
        song.setDurationSec(request.getDurationSec() == null ? 0 : Math.max(0, request.getDurationSec()));
        song.setLyrics(normalizeOptional(request.getLyrics()));
        song.setLyricsType(parseLyricsType(request.getLyricsType()));
        // New songs always start as DRAFT; only an admin can publish them after review.
        song.setStatus(SongStatus.DRAFT);

        try {
            Song created = songRepository.create(song, artistId, genreIds);
            // Persist synced lyric lines into song_lyrics so the lyrics API can serve them.
            if (created.getLyricsType() == LyricsType.SYNCED) {
                persistSyncedLyrics(created.getId(), request.getLyrics());
            }
            return withGenres(SongResponse.fromEntity(created), created.getId());
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

        List<Integer> genreIds = validateGenres(request.getGenreIds());

        Song updated = songRepository.updateOwn(artistId, songId, title.trim(), coverUrl, lyrics, lyricsType, genreIds)
                .orElseThrow(() -> {
                    SongException notFound = new SongException("SONG_NOT_FOUND", "Song was not found");
                    try {
                        SongStatus status = currentStatus(artistId, songId);
                        if (status != null && !status.isEditableByArtist()) {
                            return new SongException(
                                    "SONG_NOT_EDITABLE",
                                    "Song can only be edited while it is DRAFT or REJECTED"
                            );
                        }
                    } catch (SQLException ignored) {
                        // Fall back to the generic not-found error below.
                    }
                    return notFound;
                });

        // Keep song_lyrics in sync with the chosen mode.
        if (parseLyricsType(lyricsType) == LyricsType.SYNCED) {
            persistSyncedLyrics(songId, lyrics);
        } else {
            lyricsRepository.deleteBySongId(songId);
        }

        return withGenres(SongResponse.fromEntity(updated), songId);
    }

    /**
     * Artist submit flow: DRAFT/REJECTED → SUBMITTED. Validates that the song is
     * owned, submittable, and carries at least one genre before transitioning.
     */
    public SongResponse submitForReview(int artistId, int songId) throws SongException, SQLException {
        Song song = songRepository.findOwnedById(artistId, songId)
                .orElseThrow(() -> new SongException("SONG_NOT_FOUND", "Song was not found"));

        if (!song.getStatus().isSubmittable()) {
            throw new SongException(
                    "INVALID_STATUS_TRANSITION",
                    "Only DRAFT or REJECTED songs can be submitted for review"
            );
        }

        if (genreRepository.findForSong(songId).isEmpty()) {
            throw new SongException("SONG_GENRE_REQUIRED", "Select at least one genre before submitting");
        }

        Song submitted = songRepository.submitForReview(artistId, songId)
                .orElseThrow(() -> new SongException("SONG_NOT_FOUND", "Song was not found"));

        return withGenres(SongResponse.fromEntity(submitted), songId);
    }

    private SongStatus currentStatus(int artistId, int songId) throws SQLException {
        return songRepository.findOwnedById(artistId, songId)
                .map(Song::getStatus)
                .orElse(null);
    }

    /**
     * Validates that genreIds is a non-empty, unique list of 1-3 existing genre
     * ids, and returns the normalized (distinct) list. Backend enforcement —
     * the frontend selector is a convenience, not the source of truth.
     */
    private List<Integer> validateGenres(List<Integer> genreIds) throws SongException, SQLException {
        if (genreIds == null || genreIds.isEmpty()) {
            throw new SongException("SONG_GENRE_REQUIRED", "Select at least one genre");
        }
        List<Integer> distinct = genreIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.size() > MAX_GENRES) {
            throw new SongException(
                    "SONG_GENRE_LIMIT_EXCEEDED",
                    "A song can have at most " + MAX_GENRES + " genres"
            );
        }
        if (distinct.size() != genreIds.size()) {
            throw new SongException("INVALID_GENRE_IDS", "Genres must be unique and valid");
        }
        long found = genreRepository.countByIds(distinct);
        if (found != distinct.size()) {
            throw new SongException("INVALID_GENRE_IDS", "One or more genres do not exist");
        }
        return distinct;
    }

    private SongResponse withGenres(SongResponse response, int songId) throws SQLException {
        List<Genre> genres = genreRepository.findForSong(songId);
        response.setGenres(genres.stream().map(GenreResponse::fromEntity).toList());
        return response;
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

    public PagedResponse<SongResponse> getSongs(int artistId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<Song> songs = songRepository.getOwnedPage(artistId, size, offset);

        // Batch-load genres for the whole page in one query.
        Map<Integer, List<Genre>> genresBySong = genreRepository.findForSongs(
                songs.stream().map(Song::getId).toList());

        List<SongResponse> items = songs.stream()
                .map(song -> {
                    SongResponse response = SongResponse.fromEntity(song);
                    response.setGenres(genresBySong.getOrDefault(song.getId(), List.of())
                            .stream().map(GenreResponse::fromEntity).toList());
                    return response;
                })
                .toList();
        long total = songRepository.countOwned(artistId);

        return new PagedResponse<>(items, total, page, size);
    }

    /**
     * Resolves a song that belongs to the given artist by id and returns it as a
     * {@link SongResponse} including its genres. Throws
     * {@link SongException#SONG_NOT_FOUND} when the song does not exist or is
     * owned by a different artist. Used by Studio song editing.
     */
    public SongResponse getOwnSongResponse(int artistId, int songId) throws SongException, SQLException {
        Song song = getOwnSongById(artistId, songId);
        return withGenres(SongResponse.fromEntity(song), songId);
    }

    /**
     * Resolves a song that belongs to the given artist by id. Throws
     * {@link SongException#SONG_NOT_FOUND} when the song does not exist or is
     * owned by a different artist. Used by Studio lyrics lookup.
     */
    public Song getOwnSongById(int artistId, int songId) throws SongException, SQLException {
        return songRepository.findOwnedById(artistId, songId)
                .orElseThrow(() -> new SongException("SONG_NOT_FOUND", "Song was not found"));
    }

    /**
     * Updates synced lyrics for a song owned by the given artist. Replaces all
     * existing lyric lines. Membership is verified by the caller.
     */
    public void updateOwnSongSyncedLyrics(int artistId, int songId, SyncedLyricsRequest request)
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
