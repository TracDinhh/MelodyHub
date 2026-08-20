package com.melodyHub.service.artist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.melodyHub.dto.request.SongCreateRequest;
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
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArtistSongServiceTest {
    private ArtistSongService newService(StubSongRepository repository) {
        return newService(repository, new StubGenreRepository());
    }

    private ArtistSongService newService(StubSongRepository repository, StubGenreRepository genreRepository) {
        return new ArtistSongService(repository, new StubSongLyricsRepository(), genreRepository);
    }

    @Test
    void rejectsInvalidSyncedLyricsBeforeCreatingTheSong() {
        ArtistSongService service = newService(new StubSongRepository());
        SongCreateRequest request = new SongCreateRequest(
                "Synced Song",
                "synced-song",
                "https://example.com/song.mp3",
                null,
                180,
                "{\"lines\":[{\"startTime\":-1,\"text\":\"Too early\"}]}",
                "SYNCED",
                List.of(1)
        );

        SongException exception = assertThrows(
                SongException.class,
                () -> service.createSong(12, request)
        );

        assertEquals("INVALID_SYNCED_LYRICS", exception.getCode());
    }

    @Test
    void requiresAtLeastOneGenreWhenCreating() {
        ArtistSongService service = newService(new StubSongRepository());

        SongException exception = assertThrows(
                SongException.class,
                () -> service.createSong(12, createRequest(null))
        );

        assertEquals("SONG_GENRE_REQUIRED", exception.getCode());
    }

    @Test
    void rejectsMoreThanThreeGenresWhenCreating() {
        ArtistSongService service = newService(new StubSongRepository());

        SongException exception = assertThrows(
                SongException.class,
                () -> service.createSong(12, createRequest(List.of(1, 2, 3, 4)))
        );

        assertEquals("SONG_GENRE_LIMIT_EXCEEDED", exception.getCode());
    }

    @Test
    void rejectsUnknownGenreIdsWhenCreating() {
        StubSongRepository repository = new StubSongRepository();
        StubGenreRepository genres = new StubGenreRepository();
        genres.valid = new java.util.HashSet<>(List.of(1));
        ArtistSongService service = newService(repository, genres);

        SongException exception = assertThrows(
                SongException.class,
                () -> service.createSong(12, createRequest(List.of(1, 2)))
        );

        assertEquals("INVALID_GENRE_IDS", exception.getCode());
    }

    @Test
    void createsSongAsDraftWhenGenresAreValid() throws SQLException, SongException {
        StubSongRepository repository = new StubSongRepository();
        repository.created = song(50, "My Song", "my-song", SongStatus.DRAFT);
        ArtistSongService service = newService(repository);

        SongResponse response = service.createSong(12, createRequest(List.of(1, 4)));

        assertEquals(SongStatus.DRAFT, response.getStatus());
        assertEquals(List.of(1, 4), repository.genreIds);
        assertEquals(12, repository.artistId);
    }

    @Test
    void submitsDraftForReview() throws SQLException, SongException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Draft Song", "draft-song", SongStatus.DRAFT));
        repository.submitted = Optional.of(song(31, "Draft Song", "draft-song", SongStatus.SUBMITTED));
        StubGenreRepository genres = new StubGenreRepository();
        genres.songHasGenres = true;
        ArtistSongService service = newService(repository, genres);

        SongResponse response = service.submitForReview(12, 31);

        assertEquals(SongStatus.SUBMITTED, response.getStatus());
        assertEquals(31, repository.songId);
    }

    @Test
    void rejectsSubmittingWhenNoGenreIsSet() {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Draft Song", "draft-song", SongStatus.DRAFT));
        ArtistSongService service = newService(repository);

        SongException exception = assertThrows(
                SongException.class,
                () -> service.submitForReview(12, 31)
        );

        assertEquals("SONG_GENRE_REQUIRED", exception.getCode());
    }

    @Test
    void rejectsSubmittingAPublishedSong() {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Published Song", "published-song", SongStatus.PUBLISHED));
        ArtistSongService service = newService(repository);

        SongException exception = assertThrows(
                SongException.class,
                () -> service.submitForReview(12, 31)
        );

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
    }

    @Test
    void rejectsEditingASubmittedSong() {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Submitted Song", "submitted-song", SongStatus.SUBMITTED));
        ArtistSongService service = newService(repository);

        SongException exception = assertThrows(
                SongException.class,
                () -> service.updateOwnSong(12, 31, new com.melodyHub.dto.request.SongUpdateRequest(
                        "New title", null, null, "PLAIN", List.of(1)))
        );

        assertEquals("SONG_NOT_EDITABLE", exception.getCode());
    }

    @Test
    void allowsEditingARejectedSong() throws SQLException, SongException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Rejected Song", "rejected-song", SongStatus.REJECTED));
        repository.updated = Optional.of(song(31, "Rejected Song", "rejected-song", SongStatus.REJECTED));
        ArtistSongService service = newService(repository);

        SongResponse response = service.updateOwnSong(12, 31, new com.melodyHub.dto.request.SongUpdateRequest(
                "Rejected Song", null, null, "PLAIN", List.of(1)));

        assertEquals(SongStatus.REJECTED, response.getStatus());
        assertEquals(List.of(1), repository.genreIds);
    }

    @Test
    void preservesAllSongStatusesInThePrivatePage() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.page = List.of(
                song(31, "Draft Song", "draft-song", SongStatus.DRAFT),
                song(30, "Hidden Song", "hidden-song", SongStatus.HIDDEN),
                song(29, "Published Song", "published-song", SongStatus.PUBLISHED)
        );
        repository.total = 3;
        ArtistSongService service = newService(repository);

        PagedResponse<SongResponse> response = service.getSongs(12, 2, 10);

        assertEquals(12, repository.artistId);
        assertEquals(10, repository.size);
        assertEquals(10, repository.offset);
        assertEquals(3, response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(
                List.of(SongStatus.DRAFT, SongStatus.HIDDEN, SongStatus.PUBLISHED),
                response.getItems().stream().map(SongResponse::getStatus).toList()
        );
    }

    @Test
    void findsAnOwnedSongById() throws SQLException, SongException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Draft Song", "draft-song", SongStatus.DRAFT));
        ArtistSongService service = newService(repository);

        Song song = service.getOwnSongById(12, 31);

        assertEquals(31, song.getId());
        assertEquals(12, repository.artistId);
        assertEquals(31, repository.songId);
    }

    @Test
    void throwsWhenAnOwnedSongDoesNotExist() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.empty();
        ArtistSongService service = newService(repository);

        SongException exception = assertThrows(
                SongException.class,
                () -> service.getOwnSongById(12, 99)
        );

        assertEquals("SONG_NOT_FOUND", exception.getCode());
    }

    @Test
    void clampsTheOffsetWhenPageExceedsTheIntegerRange() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.page = List.of();
        repository.total = 0;
        ArtistSongService service = newService(repository);

        service.getSongs(12, Integer.MAX_VALUE, 50);

        assertEquals(Integer.MAX_VALUE, repository.offset);
    }

    private static SongCreateRequest createRequest(List<Integer> genreIds) {
        return new SongCreateRequest(
                "My Song",
                "my-song",
                "https://example.com/song.mp3",
                null,
                180,
                null,
                "PLAIN",
                genreIds
        );
    }

    private static Song song(int id, String title, String slug, SongStatus status) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 22, 10, 0);
        return new Song(
                id,
                title,
                slug,
                null,
                null,
                180,
                "/audio/" + slug + ".mp3",
                null,
                null,
                LyricsType.PLAIN,
                status,
                0L,
                null,
                null,
                null,
                null,
                timestamp,
                timestamp,
                null
        );
    }

    private static final class StubSongRepository extends SongRepository {
        private List<Song> page = List.of();
        private long total;
        private Optional<Song> byId = Optional.empty();
        private Optional<Song> submitted = Optional.empty();
        private Optional<Song> updated = Optional.empty();
        private Song created;
        private int artistId;
        private int songId;
        private int size;
        private int offset;
        private List<Integer> genreIds;

        @Override
        public List<Song> getOwnedPage(int artistId, int size, int offset) {
            this.artistId = artistId;
            this.size = size;
            this.offset = offset;
            return page;
        }

        @Override
        public long countOwned(int artistId) {
            this.artistId = artistId;
            return total;
        }

        @Override
        public Optional<Song> findOwnedById(int artistId, int songId) {
            this.artistId = artistId;
            this.songId = songId;
            return byId;
        }

        @Override
        public Song create(Song song, int artistId, List<Integer> genreIds) {
            this.artistId = artistId;
            this.genreIds = genreIds;
            return created;
        }

        @Override
        public Optional<Song> updateOwn(int artistId, int songId, String title, String coverUrl, String lyrics,
                                        String lyricsType, List<Integer> genreIds) {
            this.artistId = artistId;
            this.songId = songId;
            this.genreIds = genreIds;
            return updated;
        }

        @Override
        public Optional<Song> submitForReview(int artistId, int songId) {
            this.artistId = artistId;
            this.songId = songId;
            return submitted;
        }
    }

    private static final class StubGenreRepository extends GenreRepository {
        private java.util.Set<Integer> valid = new java.util.HashSet<>(List.of(1, 2, 3, 4, 5));
        private boolean songHasGenres = false;

        @Override
        public Map<Integer, List<Genre>> findForSongs(Collection<Integer> songIds) {
            return new LinkedHashMap<>();
        }

        @Override
        public List<Genre> findForSong(int songId) {
            return songHasGenres ? List.of(new Genre((short) 1, "Pop", "pop")) : List.of();
        }

        @Override
        public long countByIds(Collection<Integer> genreIds) {
            if (genreIds == null) {
                return 0L;
            }
            return genreIds.stream().filter(valid::contains).distinct().count();
        }
    }

    private static final class StubSongLyricsRepository extends SongLyricsRepository {
        @Override
        public void deleteBySongId(int songId) throws SQLException {
            // no-op: avoid hitting the real database in unit tests
        }

        @Override
        public void replaceForSong(int songId, List<SyncedLyricLine> lines) throws SQLException {
            // no-op: avoid hitting the real database in unit tests
        }
    }
}