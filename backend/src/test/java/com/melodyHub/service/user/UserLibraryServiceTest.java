package com.melodyHub.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserLibraryServiceTest {
    @Test
    void getLikedSongsComputesOffsetAndMapsArtists() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.likedSongs = List.of(song(41, "A song"), song(42, "Another song"));
        repository.artists = List.of(artist(7, "Lena"));
        repository.likedTotal = 5;
        UserLibraryService service = new UserLibraryService(repository);

        PagedResponse<SongSummaryResponse> response = service.getLikedSongs(9, 2, 20);

        assertEquals(9, repository.userId);
        assertEquals(20, repository.size);
        assertEquals(20, repository.offset);
        assertEquals(5, response.getTotal());
        assertEquals(2, response.getItems().size());
        assertEquals("Lena", response.getItems().get(0).getArtists().get(0).getName());
        assertEquals(LyricsType.SYNCED, response.getItems().get(0).getLyricsType());
    }

    @Test
    void likeSongAddsOnlyPublishedSongs() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        UserLibraryService service = new UserLibraryService(repository);

        assertFalse(service.likeSong(9, 41).isPresent());
        assertFalse(repository.addLikeCalled);

        repository.published = true;
        assertTrue(service.likeSong(9, 41).orElseThrow());
        assertTrue(repository.addLikeCalled);
        assertEquals(9, repository.userId);
        assertEquals(41, repository.songId);
    }

    @Test
    void unlikeSongRemovesOnlyPublishedSongs() throws SQLException {
        StubSongRepository repository = new StubSongRepository();
        repository.published = true;
        UserLibraryService service = new UserLibraryService(repository);

        assertFalse(service.unlikeSong(9, 41).orElseThrow());
        assertTrue(repository.removeLikeCalled);

        repository.published = false;
        assertFalse(service.unlikeSong(9, 41).isPresent());
    }

    @Test
    void getLikedSongsRejectsAnOffsetBeyondIntegerRange() {
        UserLibraryService service = new UserLibraryService(new StubSongRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getLikedSongs(9, Integer.MAX_VALUE, 50)
        );

        assertEquals("page is too large", exception.getMessage());
    }

    private static Song song(int id, String title) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 13, 10, 0);
        return new Song(
                id, title, "song-" + id, null, null, 180,
                "/audio/" + id + ".mp3", null, null, LyricsType.SYNCED, SongStatus.PUBLISHED,
                0L, timestamp, timestamp, null
        );
    }

    private static Artist artist(int id, String name) {
        Artist artist = new Artist();
        artist.setId(id);
        artist.setName(name);
        return artist;
    }

    private static final class StubSongRepository extends SongRepository {
        private List<Song> likedSongs = List.of();
        private List<Artist> artists = List.of();
        private long likedTotal;
        private boolean published;
        private boolean addLikeCalled;
        private boolean removeLikeCalled;
        private int userId;
        private int songId;
        private int size;
        private int offset;

        @Override
        public List<Song> getLikedPage(int userId, int size, int offset) {
            this.userId = userId;
            this.size = size;
            this.offset = offset;
            return likedSongs;
        }

        @Override
        public long countLikedBy(int userId) {
            this.userId = userId;
            return likedTotal;
        }

        @Override
        public List<Artist> findArtistsForSong(int songId) {
            return artists;
        }

        @Override
        public boolean isPublishedSong(int songId) {
            this.songId = songId;
            return published;
        }

        @Override
        public void addLike(int userId, int songId) {
            this.userId = userId;
            this.songId = songId;
            this.addLikeCalled = true;
        }

        @Override
        public void removeLike(int userId, int songId) {
            this.userId = userId;
            this.songId = songId;
            this.removeLikeCalled = true;
        }
    }
}
