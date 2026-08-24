package com.melodyHub.service.album;

import com.melodyHub.dto.request.AlbumCreateRequest;
import com.melodyHub.dto.request.AlbumUpdateRequest;
import com.melodyHub.dto.response.AlbumResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.entity.Album;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.AlbumException;
import com.melodyHub.repository.AlbumRepository;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class AlbumService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SLUG_LENGTH = 280;
    private static final int MAX_URL_LENGTH = 500;
    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;

    public AlbumService() {
        this(new AlbumRepository(), new ArtistRepository(), new SongRepository());
    }

    public AlbumService(AlbumRepository albumRepository, ArtistRepository artistRepository, SongRepository songRepository) {
        this.albumRepository = Objects.requireNonNull(albumRepository, "albumRepository must not be null");
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public AlbumResponse create(int artistId, AlbumCreateRequest request) throws AlbumException, SQLException {
        validateCreateRequest(request);

        String slug = generateSlug(request.getTitle());
        Album album = new Album();
        album.setArtistId(artistId);
        album.setTitle(request.getTitle().trim());
        album.setSlug(slug);
        album.setAlbumType(normalizeAlbumType(request.getAlbumType()));
        album.setCoverUrl(normalizeOptional(request.getCoverUrl()));
        album.setReleaseDate(request.getReleaseDate() != null ? request.getReleaseDate().atStartOfDay() : null);
        album.setCreatedAt(java.time.LocalDateTime.now());
        album.setUpdatedAt(java.time.LocalDateTime.now());

        try {
            Album created = albumRepository.create(album);
            return AlbumResponse.fromEntity(created, 0);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE) {
                throw new AlbumException("ALBUM_SLUG_EXISTS", "An album with this slug already exists");
            }
            throw exception;
        }
    }

    public AlbumResponse update(int artistId, int albumId, AlbumUpdateRequest request)
            throws AlbumException, SQLException {
        validateUpdateRequest(request);

        Optional<Album> existing = albumRepository.findOwnedById(artistId, albumId);
        if (existing.isEmpty()) {
            throw new AlbumException("ALBUM_NOT_FOUND", "Album was not found or you don't have access");
        }

        Album album = existing.get();
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            album.setTitle(request.getTitle().trim());
        }
        if (request.getAlbumType() != null) {
            album.setAlbumType(normalizeAlbumType(request.getAlbumType()));
        }
        if (request.getCoverUrl() != null) {
            album.setCoverUrl(normalizeOptional(request.getCoverUrl()));
        }
        if (request.getReleaseDate() != null) {
            album.setReleaseDate(request.getReleaseDate().atStartOfDay());
        }
        album.setUpdatedAt(java.time.LocalDateTime.now());

        try {
            Album updated = albumRepository.update(album);
            int songCount = songRepository.countByAlbum(albumId);
            return AlbumResponse.fromEntity(updated, songCount);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE) {
                throw new AlbumException("ALBUM_SLUG_EXISTS", "An album with this slug already exists");
            }
            throw exception;
        }
    }

    public boolean delete(int artistId, int albumId) throws AlbumException, SQLException {
        Optional<Album> existing = albumRepository.findOwnedById(artistId, albumId);
        if (existing.isEmpty()) {
            throw new AlbumException("ALBUM_NOT_FOUND", "Album was not found or you don't have access");
        }

        // Check if album has published songs - don't allow deletion
        int publishedSongs = songRepository.countPublishedByAlbum(albumId);
        if (publishedSongs > 0) {
            throw new AlbumException(
                    "ALBUM_HAS_PUBLISHED_SONGS",
                    "Cannot delete album with published songs. Remove or move songs first."
            );
        }

        return albumRepository.delete(artistId, albumId);
    }

    public Optional<AlbumResponse> getById(int artistId, int albumId) throws SQLException {
        Optional<Album> album = albumRepository.findOwnedById(artistId, albumId);
        if (album.isEmpty()) {
            return Optional.empty();
        }
        int songCount = songRepository.countByAlbum(albumId);
        return Optional.of(AlbumResponse.fromEntity(album.get(), songCount));
    }

    public PagedResponse<AlbumResponse> getPage(int artistId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<Album> albums = albumRepository.getPageByArtist(artistId, size, offset);
        long total = albumRepository.countByArtist(artistId);

        List<AlbumResponse> items = new java.util.ArrayList<>(albums.size());
        for (Album album : albums) {
            int songCount = songRepository.countByAlbum(album.getId());
            items.add(AlbumResponse.fromEntity(album, songCount));
        }

        return new PagedResponse<>(items, total, page, size);
    }

    // For public access
    public Optional<AlbumResponse> getPublicById(int albumId) throws SQLException {
        Optional<Album> album = albumRepository.findActiveById(albumId);
        if (album.isEmpty()) {
            return Optional.empty();
        }
        int songCount = songRepository.countPublishedByAlbum(albumId);
        return Optional.of(AlbumResponse.fromEntity(album.get(), songCount));
    }

    public List<AlbumResponse> getArtistAlbums(int artistId) throws SQLException {
        List<Album> albums = albumRepository.findByArtistId(artistId);
        List<AlbumResponse> responses = new java.util.ArrayList<>(albums.size());
        for (Album album : albums) {
            int songCount = songRepository.countByAlbum(album.getId());
            responses.add(AlbumResponse.fromEntity(album, songCount));
        }
        return responses;
    }

    public List<Map<String, Object>> getSongsInAlbum(int artistId, int albumId) throws SQLException, AlbumException {
        // Verify ownership
        Optional<Album> album = albumRepository.findOwnedById(artistId, albumId);
        if (album.isEmpty()) {
            throw new AlbumException("ALBUM_NOT_FOUND", "Album was not found or you don't have access");
        }

        List<com.melodyHub.entity.Song> songs = songRepository.findByAlbum(albumId);
        List<Map<String, Object>> result = new java.util.ArrayList<>(songs.size());
        for (com.melodyHub.entity.Song song : songs) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", song.getId());
            item.put("title", song.getTitle());
            item.put("slug", song.getSlug());
            item.put("coverUrl", song.getCoverUrl());
            item.put("durationSec", song.getDurationSec());
            item.put("trackNumber", song.getTrackNumber());
            item.put("status", song.getStatus() != null ? song.getStatus().name() : null);
            item.put("playCount", song.getPlayCount());
            result.add(item);
        }
        return result;
    }

    private void validateCreateRequest(AlbumCreateRequest request) throws AlbumException {
        if (request == null) {
            throw new AlbumException("INVALID_REQUEST", "Request body is required");
        }

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            throw new AlbumException("ALBUM_TITLE_REQUIRED", "Title is required");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new AlbumException("ALBUM_TITLE_TOO_LONG", "Title must be " + MAX_TITLE_LENGTH + " characters or less");
        }

        String albumType = request.getAlbumType();
        if (albumType != null && !albumType.isBlank()) {
            String normalized = normalizeAlbumType(albumType);
            if (normalized == null) {
                throw new AlbumException("INVALID_ALBUM_TYPE", "Album type must be ALBUM, EP, or SINGLE");
            }
        }

        String coverUrl = request.getCoverUrl();
        if (coverUrl != null && !coverUrl.isBlank()) {
            if (coverUrl.length() > MAX_URL_LENGTH || !isHttpUrl(coverUrl)) {
                throw new AlbumException("INVALID_COVER_URL", "Cover URL must be a valid HTTP/HTTPS URL");
            }
        }
    }

    private void validateUpdateRequest(AlbumUpdateRequest request) throws AlbumException {
        if (request == null) {
            throw new AlbumException("INVALID_REQUEST", "Request body is required");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            if (request.getTitle().trim().length() > MAX_TITLE_LENGTH) {
                throw new AlbumException("ALBUM_TITLE_TOO_LONG", "Title must be " + MAX_TITLE_LENGTH + " characters or less");
            }
        }

        if (request.getAlbumType() != null) {
            String normalized = normalizeAlbumType(request.getAlbumType());
            if (normalized == null) {
                throw new AlbumException("INVALID_ALBUM_TYPE", "Album type must be ALBUM, EP, or SINGLE");
            }
        }

        String coverUrl = request.getCoverUrl();
        if (coverUrl != null && !coverUrl.isBlank()) {
            if (coverUrl.length() > MAX_URL_LENGTH || !isHttpUrl(coverUrl)) {
                throw new AlbumException("INVALID_COVER_URL", "Cover URL must be a valid HTTP/HTTPS URL");
            }
        }
    }

    private String normalizeAlbumType(String type) {
        if (type == null || type.isBlank()) {
            return "ALBUM";
        }
        String upper = type.trim().toUpperCase();
        if (upper.equals("ALBUM") || upper.equals("EP") || upper.equals("SINGLE")) {
            return upper;
        }
        return null;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String slug = title.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }

    private boolean isHttpUrl(String value) {
        try {
            java.net.URI uri = new java.net.URI(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (java.net.URISyntaxException exception) {
            return false;
        }
    }
}
