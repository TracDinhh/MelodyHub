package com.melodyHub.service.artist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.melodyHub.repository.ArtistFollowRepository;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArtistStatsServiceTest {
    @Test
    void getStatsIncludesTheArtistsFollowerCount() throws SQLException {
        StubSongRepository songRepository = new StubSongRepository();
        StubArtistFollowRepository followRepository = new StubArtistFollowRepository();
        followRepository.followerCount = 37L;
        ArtistStatsService service = new ArtistStatsService(songRepository, followRepository);

        Map<String, Object> stats = service.getStats(12);

        assertEquals(4L, stats.get("totalSongs"));
        assertEquals(37L, stats.get("totalFollowers"));
        assertEquals(12, followRepository.artistId);
    }

    private static final class StubSongRepository extends SongRepository {
        @Override
        public Map<String, Object> getArtistStats(int artistId) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalSongs", 4L);
            return stats;
        }
    }

    private static final class StubArtistFollowRepository extends ArtistFollowRepository {
        private int artistId;
        private long followerCount;

        @Override
        public long countFollowers(int artistId) {
            this.artistId = artistId;
            return followerCount;
        }
    }
}
