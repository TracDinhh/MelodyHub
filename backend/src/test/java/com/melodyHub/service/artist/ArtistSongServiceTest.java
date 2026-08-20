package com.melodyHub.service.artist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.melodyHub.dto.request.SongCreateRequest;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.exception.SongException;
import com.melodyHub.repository.SongLyricsRepository;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArtistSongServiceTest {
    @Test
    void rejectsInvalidSyncedLyricsBeforeCreatingTheSong() {
        ArtistSongService service = new ArtistSongService(new StubSongRepository(), new SongLyricsRepository());
        SongCreateRequest request = new SongCreateRequest(
                "Synced Song",
                "synced-song",
                "https://example.com/song.mp3",
                null,
                180,
                "{\"lines\":[{\"startTime\":-1,\"text\":\"Too early\"}]}",
                "SYNCED"
        );

        SongException exception = assertThrows(
                SongException.class,
                () -> service.createSong(12, request)
        );

        assertEquals("INVALID_SYNCED_LYRICS", exception.getCode());
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
        ArtistSongService service = new ArtistSongService(repository, new SongLyricsRepository());

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
        ArtistSongService service = new ArtistSongService(repository, new SongLyricsRepository());

        Song song = service.getOwnSongById(12, 31);

        assertEquals(31, song.getId());
        assertEquals(12, repository.artistId);
        assertEquals(31, repository.songId);
    }

    @Test
    void throwsWhenAnOwnedSongDoesNotExist() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.byId = Optional.empty();
        ArtistSongService service = new ArtistSongService(repository, new SongLyricsRepository());

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
        ArtistSongService service = new ArtistSongService(repository, new SongLyricsRepository());

        service.getSongs(12, Integer.MAX_VALUE, 50);

        assertEquals(Integer.MAX_VALUE, repository.offset);
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
                timestamp,
                timestamp,
                null
        );
    }

    private static final class StubSongRepository extends SongRepository {
        private List<Song> page = List.of();
        private long total;
        private Optional<Song> byId = Optional.empty();
        private int artistId;
        private int songId;
        private int size;
        private int offset;

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
    }
}