package com.melodyHub.service.album;

import com.melodyHub.dto.request.AlbumCreateRequest;
import com.melodyHub.dto.request.AlbumUpdateRequest;
import com.melodyHub.dto.response.AlbumResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Album;
import com.melodyHub.exception.AlbumException;
import com.melodyHub.repository.AlbumRepository;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.SongRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlbumServiceTest {
    private AlbumService newService(StubAlbumRepository albumRepo, StubSongRepository songRepo) {
        return new AlbumService(albumRepo, new StubArtistRepository(), songRepo);
    }

    @Test
    void createsAlbumSuccessfully() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.created = album(1, "My Album", "my-album");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest(
                "My Album",
                "ALBUM",
                null,
                LocalDate.of(2026, 1, 15)
        );

        AlbumResponse response = service.create(1, request);

        assertEquals("My Album", response.getTitle());
        assertEquals("my-album", response.getSlug());
        assertEquals("ALBUM", response.getAlbumType());
        assertEquals(1, albumRepo.artistId);
    }

    @Test
    void createsAlbumWithEPType() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.created = album(1, "My EP", "my-ep");
        albumRepo.created.setAlbumType("EP");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest("My EP", "EP", null, null);
        AlbumResponse response = service.create(1, request);

        assertEquals("EP", response.getAlbumType());
    }

    @Test
    void createsAlbumWithSingleType() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.created = album(1, "My Single", "my-single");
        albumRepo.created.setAlbumType("SINGLE");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest("My Single", "SINGLE", null, null);
        AlbumResponse response = service.create(1, request);

        assertEquals("SINGLE", response.getAlbumType());
    }

    @Test
    void defaultsAlbumTypeToAlbum() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.created = album(1, "No Type Album", "no-type-album");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest("No Type Album", null, null, null);
        AlbumResponse response = service.create(1, request);

        assertEquals("ALBUM", response.getAlbumType());
    }

    @Test
    void rejectsAlbumWithoutTitle() {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest("", null, null, null);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.create(1, request));
        assertEquals("ALBUM_TITLE_REQUIRED", exception.getCode());
    }

    @Test
    void rejectsAlbumWithNullTitle() {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest(null, null, null, null);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.create(1, request));
        assertEquals("ALBUM_TITLE_REQUIRED", exception.getCode());
    }

    @Test
    void rejectsAlbumWithInvalidAlbumType() {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        AlbumCreateRequest request = new AlbumCreateRequest("Bad Album", "INVALID_TYPE", null, null);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.create(1, request));
        assertEquals("INVALID_ALBUM_TYPE", exception.getCode());
    }

    @Test
    void rejectsAlbumWithTooLongTitle() {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        String longTitle = "A".repeat(256);
        AlbumCreateRequest request = new AlbumCreateRequest(longTitle, null, null, null);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.create(1, request));
        assertEquals("ALBUM_TITLE_TOO_LONG", exception.getCode());
    }

    @Test
    void updatesAlbumTitle() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.existing = album(1, "Old Title", "old-title");
        albumRepo.updated = album(1, "New Title", "old-title");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumUpdateRequest request = new AlbumUpdateRequest("New Title", null, null, null);
        AlbumResponse response = service.update(1, 1, request);

        assertEquals("New Title", response.getTitle());
    }

    @Test
    void updatesAlbumType() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.existing = album(1, "My Album", "my-album");
        albumRepo.updated = album(1, "My Album", "my-album");
        albumRepo.updated.setAlbumType("EP");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumUpdateRequest request = new AlbumUpdateRequest(null, "EP", null, null);
        AlbumResponse response = service.update(1, 1, request);

        assertEquals("EP", response.getAlbumType());
    }

    @Test
    void updatesAlbumCoverUrl() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        StubSongRepository songRepo = new StubSongRepository();
        albumRepo.existing = album(1, "My Album", "my-album");
        albumRepo.updated = album(1, "My Album", "my-album");
        albumRepo.updated.setCoverUrl("https://example.com/cover.jpg");
        AlbumService service = newService(albumRepo, songRepo);

        AlbumUpdateRequest request = new AlbumUpdateRequest(null, null, "https://example.com/cover.jpg", null);
        AlbumResponse response = service.update(1, 1, request);

        assertEquals("https://example.com/cover.jpg", response.getCoverUrl());
    }

    @Test
    void rejectsUpdateForNonOwnedAlbum() {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.existing = null;
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        AlbumUpdateRequest request = new AlbumUpdateRequest("New Title", null, null, null);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.update(1, 99, request));
        assertEquals("ALBUM_NOT_FOUND", exception.getCode());
    }

    @Test
    void deletesAlbumSuccessfully() throws AlbumException, SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.existing = album(1, "To Delete", "to-delete");
        albumRepo.deleted = true;
        StubSongRepository songRepo = new StubSongRepository();
        songRepo.albumSongCount = 0;
        songRepo.publishedAlbumSongCount = 0;
        AlbumService service = newService(albumRepo, songRepo);

        boolean result = service.delete(1, 1);

        assertTrue(result);
    }

    @Test
    void rejectsDeleteOfAlbumWithPublishedSongs() throws SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.existing = album(1, "Has Songs", "has-songs");
        StubSongRepository songRepo = new StubSongRepository();
        songRepo.albumSongCount = 5;
        songRepo.publishedAlbumSongCount = 3;
        AlbumService service = newService(albumRepo, songRepo);

        AlbumException exception = assertThrows(AlbumException.class, () -> service.delete(1, 1));
        assertEquals("ALBUM_HAS_PUBLISHED_SONGS", exception.getCode());
    }

    @Test
    void getsPagedAlbums() throws SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.page = List.of(
                album(1, "Album 1", "album-1"),
                album(2, "Album 2", "album-2")
        );
        albumRepo.total = 2;
        StubSongRepository songRepo = new StubSongRepository();
        songRepo.albumSongCount = 3;
        AlbumService service = newService(albumRepo, songRepo);

        PagedResponse<AlbumResponse> response = service.getPage(1, 1, 20);

        assertEquals(2, response.getItems().size());
        assertEquals(2, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void getsAlbumById() throws SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.existing = album(1, "My Album", "my-album");
        StubSongRepository songRepo = new StubSongRepository();
        songRepo.albumSongCount = 5;
        AlbumService service = newService(albumRepo, songRepo);

        Optional<AlbumResponse> response = service.getById(1, 1);

        assertTrue(response.isPresent());
        assertEquals("My Album", response.get().getTitle());
        assertEquals(5, response.get().getSongCount());
    }

    @Test
    void returnsEmptyForNonExistentAlbum() throws SQLException {
        StubAlbumRepository albumRepo = new StubAlbumRepository();
        albumRepo.existing = null;
        StubSongRepository songRepo = new StubSongRepository();
        AlbumService service = newService(albumRepo, songRepo);

        Optional<AlbumResponse> response = service.getById(1, 99);

        assertTrue(response.isEmpty());
    }

    // Helper methods
    private Album album(int id, String title, String slug) {
        LocalDateTime now = LocalDateTime.now();
        Album album = new Album();
        album.setId(id);
        album.setTitle(title);
        album.setSlug(slug);
        album.setAlbumType("ALBUM");
        album.setArtistId(1);
        album.setCreatedAt(now);
        album.setUpdatedAt(now);
        return album;
    }

    // Stubs
    private static final class StubAlbumRepository extends AlbumRepository {
        public Album created;
        public Album existing;
        public Album updated;
        public boolean deleted;
        public List<Album> page;
        public long total;
        public int artistId;
        public int albumId;

        @Override
        public Album create(Album album) {
            artistId = album.getArtistId();
            return created;
        }

        @Override
        public Optional<Album> findOwnedById(int artistId, int albumId) {
            this.artistId = artistId;
            this.albumId = albumId;
            return Optional.ofNullable(existing);
        }

        @Override
        public Album update(Album album) {
            return updated != null ? updated : album;
        }

        @Override
        public boolean delete(int artistId, int albumId) {
            this.artistId = artistId;
            this.albumId = albumId;
            return deleted;
        }

        @Override
        public List<Album> getPageByArtist(int artistId, int limit, int offset) {
            this.artistId = artistId;
            return page != null ? page : List.of();
        }

        @Override
        public long countByArtist(int artistId) {
            this.artistId = artistId;
            return total;
        }
    }

    private static final class StubSongRepository extends SongRepository {
        public int albumSongCount;
        public int publishedAlbumSongCount;

        @Override
        public int countByAlbum(int albumId) {
            return albumSongCount;
        }

        @Override
        public int countPublishedByAlbum(int albumId) {
            return publishedAlbumSongCount;
        }
    }

    private static final class StubArtistRepository extends ArtistRepository {
        // No-op for tests
    }
}
