package com.melodyHub.service.user;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongSummaryResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.Song;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** User-owned library operations backed by song_likes. */
public class UserLibraryService {
    private final SongRepository songRepository;

    public UserLibraryService() {
        this(new SongRepository());
    }

    public UserLibraryService(SongRepository songRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public PagedResponse<SongSummaryResponse> getLikedSongs(int userId, int page, int size) throws SQLException {
        int offset = offset(page, size);
        List<Song> songs = songRepository.getLikedPage(userId, size, offset);
        List<SongSummaryResponse> items = new ArrayList<>(songs.size());
        for (Song song : songs) {
            List<Artist> artists = songRepository.findArtistsForSong(song.getId());
            items.add(SongSummaryResponse.build(song, artists));
        }
        return new PagedResponse<>(items, songRepository.countLikedBy(userId), page, size);
    }

    public Optional<Boolean> likeSong(int userId, int songId) throws SQLException {
        if (!songRepository.isPublishedSong(songId)) {
            return Optional.empty();
        }
        songRepository.addLike(userId, songId);
        return Optional.of(true);
    }

    public Optional<Boolean> unlikeSong(int userId, int songId) throws SQLException {
        if (!songRepository.isPublishedSong(songId)) {
            return Optional.empty();
        }
        songRepository.removeLike(userId, songId);
        return Optional.of(false);
    }

    private int offset(int page, int size) {
        long offset = (long) (page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page is too large");
        }
        return (int) offset;
    }
}
