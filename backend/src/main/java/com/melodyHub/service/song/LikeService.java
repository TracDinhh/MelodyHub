package com.melodyHub.service.song;

import com.melodyHub.dto.response.LikedSongResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.repository.SongRepository.LikedSongRow;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Write + list side of song likes. Read-side like counts on song detail live
 * in {@link SongService}; this owns like/unlike toggles and the "my liked
 * songs" list.
 */
public class LikeService {
    private final SongRepository songRepository;

    public LikeService() {
        this(new SongRepository());
    }

    public LikeService(SongRepository songRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    /** Likes a song. Idempotent. Returns true if a new like was created. */
    public boolean like(int userId, int songId) throws SQLException {
        return songRepository.like(songId, userId);
    }

    /** Unlikes a song. Returns true if a like was removed. */
    public boolean unlike(int userId, int songId) throws SQLException {
        return songRepository.unlike(songId, userId);
    }

    /** Ids of every song the user has liked — used to hydrate frontend state. */
    public List<Integer> likedSongIds(int userId) throws SQLException {
        return songRepository.findLikedSongIds(userId);
    }

    public PagedResponse<LikedSongResponse> getPage(int userId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<LikedSongRow> rows = songRepository.findLikedByUser(userId, size, offset);
        long total = songRepository.countLikedByUser(userId);

        // Batch-load artists for all songs on the page in a single query (avoids N+1).
        List<Integer> songIds = rows.stream().map(row -> row.song().getId()).toList();
        Map<Integer, List<Artist>> artistsBySong = songRepository.findArtistsForSongs(songIds);

        List<LikedSongResponse> items = new ArrayList<>(rows.size());
        for (LikedSongRow row : rows) {
            items.add(LikedSongResponse.fromEntity(
                    row.song(),
                    row.likedAt(),
                    artistsBySong.getOrDefault(row.song().getId(), List.of())
            ));
        }
        return new PagedResponse<>(items, total, page, size);
    }
}
