package com.melodyHub.service.artist;

import com.melodyHub.repository.ArtistFollowRepository;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides aggregated statistics and analytics for an artist's own songs.
 * Used by the Artist dashboard / overview page.
 */
public class ArtistStatsService {
    private final SongRepository songRepository;
    private final ArtistFollowRepository followRepository;

    public ArtistStatsService() {
        this(new SongRepository(), new ArtistFollowRepository());
    }

    public ArtistStatsService(SongRepository songRepository) {
        this(songRepository, new ArtistFollowRepository());
    }

    public ArtistStatsService(SongRepository songRepository, ArtistFollowRepository followRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
        this.followRepository = Objects.requireNonNull(followRepository, "followRepository must not be null");
    }

    /**
     * Summary statistics for the artist overview cards.
     */
    public Map<String, Object> getStats(int artistId) throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>(songRepository.getArtistStats(artistId));
        stats.put("totalFollowers", followRepository.countFollowers(artistId));
        return stats;
    }

    /**
     * Time-series and breakdown analytics for the artist dashboard charts.
     */
    public Map<String, Object> getAnalytics(int artistId) throws SQLException {
        List<Map<String, Object>> listensByDay = songRepository.getArtistListensByDay(artistId, 30);
        List<Map<String, Object>> likesByDay = songRepository.getArtistLikesByDay(artistId, 30);
        List<Map<String, Object>> topSongs = songRepository.getArtistTopSongs(artistId, 10);
        List<Map<String, Object>> songsByStatus = songRepository.getArtistSongsByStatus(artistId);

        return Map.of(
                "listensByDay", listensByDay,
                "likesByDay", likesByDay,
                "topSongs", topSongs,
                "songsByStatus", songsByStatus
        );
    }
}
