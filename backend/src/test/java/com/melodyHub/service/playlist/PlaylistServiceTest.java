package com.melodyHub.service.playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.PlaylistDetailResponse;
import com.melodyHub.dto.response.PlaylistResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.LyricsType;
import com.melodyHub.entity.Playlist;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.SongStatus;
import com.melodyHub.repository.PlaylistRepository;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaylistServiceTest {
    @Test
    void createTrimsTheNameAndDefaultsIsPublicToFalse() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.created = playlist(7, "My mix", 12);
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        PlaylistResponse response = service.create(12, "  My mix  ", "  a vibe  ", "   ", null);

        assertEquals("My mix", playlistRepository.createdArg.getName());
        assertEquals("a vibe", playlistRepository.createdArg.getDescription());
        assertEquals(12, playlistRepository.createdArg.getUserId());
        // blank cover collapses to null; null isPublic defaults to false
        assertFalse(playlistRepository.createdArg.isPublic());
        assertEquals(0, response.getSongCount());
    }

    @Test
    void createRejectsABlankName() {
        PlaylistService service = new PlaylistService(new StubPlaylistRepository(), new StubSongRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(12, "   ", null, null, false)
        );

        assertEquals("name is required", exception.getMessage());
    }

    @Test
    void createRejectsANameOverTheLengthLimit() {
        PlaylistService service = new PlaylistService(new StubPlaylistRepository(), new StubSongRepository());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(12, "x".repeat(151), null, null, false)
        );
    }

    @Test
    void getPageComputesTheOffsetAndAttachesSongCounts() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.page = List.of(playlist(1, "A", 12), playlist(2, "B", 12));
        playlistRepository.total = 5;
        playlistRepository.songCount = 3;
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        PagedResponse<PlaylistResponse> response = service.getPage(12, 2, 10);

        assertEquals(12, playlistRepository.userId);
        assertEquals(10, playlistRepository.size);
        assertEquals(10, playlistRepository.offset);
        assertEquals(5, response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(List.of(3, 3), response.getItems().stream().map(PlaylistResponse::getSongCount).toList());
    }

    @Test
    void getPageRejectsAnOffsetBeyondIntegerRange() {
        PlaylistService service = new PlaylistService(new StubPlaylistRepository(), new StubSongRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPage(12, Integer.MAX_VALUE, 50)
        );

        assertEquals("page is too large", exception.getMessage());
    }

    @Test
    void getDetailReturnsEmptyWhenThePlaylistIsNotOwned() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.byId = Optional.empty();
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        assertTrue(service.getDetail(12, 99).isEmpty());
    }

    @Test
    void getDetailMapsSongsWithTheirArtists() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.byId = Optional.of(playlist(7, "Roadtrip", 12));
        playlistRepository.songs = List.of(song(101, "Track one"), song(102, "Track two"));
        StubSongRepository songRepository = new StubSongRepository();
        songRepository.artists = List.of(artist(3, "Nova"));
        PlaylistService service = new PlaylistService(playlistRepository, songRepository);

        Optional<PlaylistDetailResponse> response = service.getDetail(12, 7);

        assertTrue(response.isPresent());
        assertEquals(2, response.get().getSongs().size());
        assertEquals(2, response.get().getSongCount());
        assertEquals("Nova", response.get().getSongs().get(0).getArtists().get(0).getName());
    }

    @Test
    void updateReturnsEmptyWhenThePlaylistIsNotOwned() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.updated = Optional.empty();
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        assertTrue(service.update(12, 7, "New name", null, null, true).isEmpty());
    }

    @Test
    void addSongRejectsAMissingSongId() {
        PlaylistService service = new PlaylistService(new StubPlaylistRepository(), new StubSongRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addSong(12, 7, null)
        );

        assertEquals("songId is required", exception.getMessage());
    }

    @Test
    void addSongReturnsEmptyWhenThePlaylistIsNotOwned() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.byId = Optional.empty();
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        assertTrue(service.addSong(12, 7, 55).isEmpty());
    }

    @Test
    void addSongDelegatesToTheRepositoryWhenOwned() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.byId = Optional.of(playlist(7, "Owned", 12));
        playlistRepository.addResult = true;
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        Optional<Boolean> response = service.addSong(12, 7, 55);

        assertTrue(response.isPresent());
        assertTrue(response.get());
        assertEquals(7, playlistRepository.addedPlaylistId);
        assertEquals(55, playlistRepository.addedSongId);
    }

    @Test
    void removeSongReturnsEmptyWhenThePlaylistIsNotOwned() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.byId = Optional.empty();
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        assertTrue(service.removeSong(12, 7, 55).isEmpty());
    }

    @Test
    void deleteDelegatesToTheRepository() throws SQLException {
        StubPlaylistRepository playlistRepository = new StubPlaylistRepository();
        playlistRepository.deleteResult = true;
        PlaylistService service = new PlaylistService(playlistRepository, new StubSongRepository());

        assertTrue(service.delete(12, 7));
        assertEquals(12, playlistRepository.userId);
        assertEquals(7, playlistRepository.deletedPlaylistId);
    }

    // ---- fixtures -------------------------------------------------------

    private static Playlist playlist(int id, String name, int userId) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 22, 10, 0);
        return new Playlist(id, name, "desc", userId, null, false, timestamp, timestamp);
    }

    private static Song song(int id, String title) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 22, 10, 0);
        return new Song(
                id, title, "slug-" + id, null, null, 180,
                "/audio/" + id + ".mp3", null, null, LyricsType.PLAIN, SongStatus.PUBLISHED,
                0L, timestamp, timestamp, null
        );
    }

    private static Artist artist(int id, String name) {
        Artist artist = new Artist();
        artist.setId(id);
        artist.setName(name);
        return artist;
    }

    private static final class StubPlaylistRepository extends PlaylistRepository {
        private Playlist created;
        private Playlist createdArg;
        private List<Playlist> page = List.of();
        private long total;
        private int songCount;
        private Optional<Playlist> byId = Optional.empty();
        private Optional<Playlist> updated = Optional.empty();
        private List<Song> songs = List.of();
        private boolean addResult;
        private boolean deleteResult;

        private int userId;
        private int size;
        private int offset;
        private int addedPlaylistId;
        private int addedSongId;
        private int deletedPlaylistId;

        @Override
        public Playlist create(Playlist playlist) {
            this.createdArg = playlist;
            return created;
        }

        @Override
        public List<Playlist> getPageByUser(int userId, int size, int offset) {
            this.userId = userId;
            this.size = size;
            this.offset = offset;
            return page;
        }

        @Override
        public long countByUser(int userId) {
            this.userId = userId;
            return total;
        }

        @Override
        public int countSongs(int playlistId) {
            return songCount;
        }

        @Override
        public Optional<Playlist> findByIdForUser(int userId, int playlistId) {
            this.userId = userId;
            return byId;
        }

        @Override
        public Optional<Playlist> update(int userId, int playlistId, String name, String description,
                String coverUrl, boolean isPublic) {
            this.userId = userId;
            return updated;
        }

        @Override
        public boolean delete(int userId, int playlistId) {
            this.userId = userId;
            this.deletedPlaylistId = playlistId;
            return deleteResult;
        }

        @Override
        public List<Song> getSongs(int playlistId) {
            return songs;
        }

        @Override
        public boolean addSong(int playlistId, int songId) {
            this.addedPlaylistId = playlistId;
            this.addedSongId = songId;
            return addResult;
        }
    }

    private static final class StubSongRepository extends SongRepository {
        private List<Artist> artists = List.of();

        @Override
        public List<Artist> findArtistsForSong(int songId) {
            return artists;
        }
    }
}
