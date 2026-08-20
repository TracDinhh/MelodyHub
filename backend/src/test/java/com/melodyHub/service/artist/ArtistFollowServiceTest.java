package com.melodyHub.service.artist;

import com.melodyHub.dto.response.ArtistPublicResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.repository.ArtistFollowRepository;
import com.melodyHub.repository.ArtistRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtistFollowServiceTest {

    @Test
    void followsAnExistingArtistAndReturnsTheNewState() throws Exception {
        StubFollowRepository follows = new StubFollowRepository();
        follows.artistExists = true;
        follows.followingResult = true;
        follows.followerCount = 42L;
        ArtistFollowService service = new ArtistFollowService(follows, new StubArtistRepository(true));

        ArtistFollowService.FollowState state = service.follow(7, 99);

        assertEquals(true, state.following());
        assertEquals(42L, state.followerCount());
        assertEquals(7, follows.userId);
        assertEquals(99, follows.artistId);
    }

    @Test
    void rejectsFollowingAMissingArtist() {
        ArtistFollowService service = new ArtistFollowService(new StubFollowRepository(), new StubArtistRepository(false));

        ArtistException exception = assertThrows(
                ArtistException.class,
                () -> service.follow(7, 99)
        );

        assertEquals("ARTIST_NOT_FOUND", exception.getCode());
    }

    @Test
    void unfollowsAndReturnsTheNewState() throws Exception {
        StubFollowRepository follows = new StubFollowRepository();
        follows.artistExists = true;
        follows.followingResult = false;
        follows.followerCount = 41L;
        ArtistFollowService service = new ArtistFollowService(follows, new StubArtistRepository(true));

        ArtistFollowService.FollowState state = service.unfollow(7, 99);

        assertEquals(false, state.following());
        assertEquals(41L, state.followerCount());
        assertEquals(7, follows.userId);
        assertEquals(99, follows.artistId);
    }

    @Test
    void rejectsUnfollowingAMissingArtist() {
        ArtistFollowService service = new ArtistFollowService(new StubFollowRepository(), new StubArtistRepository(false));

        ArtistException exception = assertThrows(
                ArtistException.class,
                () -> service.unfollow(7, 99)
        );

        assertEquals("ARTIST_NOT_FOUND", exception.getCode());
    }

    @Test
    void returnsFollowedArtistIdsForHydration() throws Exception {
        StubFollowRepository follows = new StubFollowRepository();
        follows.followingIds = List.of(3, 5, 8);
        ArtistFollowService service = new ArtistFollowService(follows, new StubArtistRepository(false));

        List<Integer> ids = service.followingIds(7);

        assertEquals(List.of(3, 5, 8), ids);
        assertEquals(7, follows.userId);
    }

    @Test
    void returnsFollowingPageWithFollowerCounts() throws Exception {
        StubFollowRepository follows = new StubFollowRepository();
        follows.artists = List.of(artist(3, "Lena Rivers"), artist(5, "Eli Vale"));
        follows.total = 2;
        follows.followerCounts = Map.of(3, 10L, 5, 20L);
        ArtistFollowService service = new ArtistFollowService(follows, new StubArtistRepository(false));

        PagedResponse<ArtistPublicResponse> response = service.getFollowingPage(7, 1, 10);

        assertEquals(2, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(7, follows.userId);
        assertEquals(10L, response.getItems().get(0).getFollowerCount());
        assertEquals(20L, response.getItems().get(1).getFollowerCount());
        assertEquals("Lena Rivers", response.getItems().get(0).getName());
    }

    @Test
    void defaultsFollowerCountToZeroWhenNoRowsExist() throws Exception {
        StubFollowRepository follows = new StubFollowRepository();
        follows.artists = List.of(artist(3, "Lena Rivers"));
        follows.total = 1;
        ArtistFollowService service = new ArtistFollowService(follows, new StubArtistRepository(false));

        PagedResponse<ArtistPublicResponse> response = service.getFollowingPage(7, 1, 10);

        assertEquals(0L, response.getItems().get(0).getFollowerCount());
    }

    // ---- fixtures -------------------------------------------------------

    private static Artist artist(int id, String name) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 22, 10, 0);
        return new Artist(id, name, "slug-" + id, null, null, timestamp, timestamp, null);
    }

    private static final class StubFollowRepository extends ArtistFollowRepository {
        private boolean artistExists;
        private boolean followingResult;
        private long followerCount;
        private int userId;
        private int artistId;
        private List<Integer> followingIds = List.of();
        private List<Artist> artists = List.of();
        private long total;
        private Map<Integer, Long> followerCounts = new LinkedHashMap<>();

        @Override
        public boolean follow(int artistId, int userId) {
            this.artistId = artistId;
            this.userId = userId;
            return followingResult;
        }

        @Override
        public boolean unfollow(int artistId, int userId) {
            this.artistId = artistId;
            this.userId = userId;
            return followingResult;
        }

        @Override
        public boolean isFollowing(int artistId, int userId) {
            this.artistId = artistId;
            this.userId = userId;
            return followingResult;
        }

        @Override
        public long countFollowers(int artistId) {
            this.artistId = artistId;
            return followerCount;
        }

        @Override
        public List<Integer> findFollowingArtistIds(int userId) {
            this.userId = userId;
            return followingIds;
        }

        @Override
        public long countFollowing(int userId) {
            this.userId = userId;
            return total;
        }

        @Override
        public List<Artist> findFollowingPage(int userId, int size, int offset) {
            this.userId = userId;
            return artists;
        }

        @Override
        public Map<Integer, Long> countFollowersForArtists(Collection<Integer> artistIds) {
            return followerCounts;
        }
    }

    private static final class StubArtistRepository extends ArtistRepository {
        private final boolean exists;

        private StubArtistRepository(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean existsActiveById(int id) {
            return exists;
        }
    }
}