package com.melodyHub.service.artist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArtistSongServiceTest {
    @Test
    void preservesAllSongStatusesInThePrivatePage() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.page = List.of(
                song(31, "Draft Song", "draft-song", SongStatus.DRAFT),
                song(30, "Hidden Song", "hidden-song", SongStatus.HIDDEN),
                song(29, "Published Song", "published-song", SongStatus.PUBLISHED)
        );
        repository.total = 3;
        ArtistSongService service = new ArtistSongService(repository);

        PagedResponse<SongResponse> response = service.getOwnPage(12, 2, 10);

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
    void findsAnOwnedSongById() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.of(song(31, "Draft Song", "draft-song", SongStatus.DRAFT));
        ArtistSongService service = new ArtistSongService(repository);

        Optional<SongResponse> response = service.getOwnByIdentifier(12, "31");

        assertTrue(response.isPresent());
        assertEquals(31, response.get().getId());
        assertEquals(12, repository.artistId);
        assertEquals(31, repository.songId);
    }

    @Test
    void findsAnOwnedSongBySlugIncludingANumericSlugFallback() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.bySlug = Optional.of(song(32, "2026", "2026", SongStatus.HIDDEN));
        ArtistSongService service = new ArtistSongService(repository);

        Optional<SongResponse> response = service.getOwnByIdentifier(12, "2026");

        assertTrue(response.isPresent());
        assertEquals("2026", response.get().getSlug());
        assertEquals("2026", repository.slug);
    }

    @Test
    void rejectsAPageWhoseOffsetExceedsTheRepositoryIntegerRange() {
        ArtistSongService service = new ArtistSongService(new StubSongRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getOwnPage(12, Integer.MAX_VALUE, 50)
        );

        assertEquals("page is too large", exception.getMessage());
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
                status,
                0L,
                timestamp,
                timestamp,
                null
        );
    }

    private static final class StubSongRepository extends SongRepository {
        private List<Song> page = List.of();
        private long total;
        private Optional<Song> byId = Optional.empty();
        private Optional<Song> bySlug = Optional.empty();
        private int artistId;
        private int songId;
        private int size;
        private int offset;
        private String slug;

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
        public Optional<Song> findOwnedBySlug(int artistId, String slug) {
            this.artistId = artistId;
            this.slug = slug;
            return bySlug;
        }
    }
}
