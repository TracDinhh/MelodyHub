package com.melodyHub.service.artist;

import com.melodyHub.dto.response.ArtistPublicResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.repository.ArtistFollowRepository;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Write + list side of artist follows. Read-side follower counts on the
 * public artist detail live in {@link PublicArtistService}; this owns the
 * follow/unfollow toggles and the "my followed artists" list.
 */
public class ArtistFollowService {
    private final ArtistFollowRepository followRepository;
    private final ArtistRepository artistRepository;

    public ArtistFollowService() {
        this(new ArtistFollowRepository(), new ArtistRepository());
    }

    public ArtistFollowService(ArtistFollowRepository followRepository, ArtistRepository artistRepository) {
        this.followRepository = Objects.requireNonNull(followRepository, "followRepository must not be null");
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
    }

    /**
     * Follows an artist. Idempotent. Throws {@link ArtistException} with
     * {@code ARTIST_NOT_FOUND} when the artist does not exist or is soft-deleted.
     */
    public FollowState follow(int userId, int artistId) throws SQLException, ArtistException {
        ensureArtistExists(artistId);
        followRepository.follow(artistId, userId);
        return state(artistId, userId);
    }

    /**
     * Unfollows an artist. Idempotent. Throws {@link ArtistException} with
     * {@code ARTIST_NOT_FOUND} when the artist does not exist or is soft-deleted.
     */
    public FollowState unfollow(int userId, int artistId) throws SQLException, ArtistException {
        ensureArtistExists(artistId);
        followRepository.unfollow(artistId, userId);
        return state(artistId, userId);
    }

    /** Ids of every artist the user follows — used to hydrate frontend state. */
    public List<Integer> followingIds(int userId) throws SQLException {
        return followRepository.findFollowingArtistIds(userId);
    }

    public long countFollowers(int artistId) throws SQLException {
        return followRepository.countFollowers(artistId);
    }

    public boolean isFollowing(int userId, int artistId) throws SQLException {
        return followRepository.isFollowing(artistId, userId);
    }

    public PagedResponse<ArtistPublicResponse> getFollowingPage(int userId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<Artist> artists = followRepository.findFollowingPage(userId, size, offset);
        long total = followRepository.countFollowing(userId);

        // Batch-load follower counts for the whole page in a single query (avoids N+1).
        Map<Integer, Long> counts = followRepository.countFollowersForArtists(
                artists.stream().map(Artist::getId).toList());

        List<ArtistPublicResponse> items = new ArrayList<>(artists.size());
        for (Artist artist : artists) {
            ArtistPublicResponse response = ArtistPublicResponse.fromEntity(artist);
            response.setFollowerCount(counts.getOrDefault(artist.getId(), 0L));
            items.add(response);
        }
        return new PagedResponse<>(items, total, page, size);
    }

    private void ensureArtistExists(int artistId) throws SQLException, ArtistException {
        if (!artistRepository.existsActiveById(artistId)) {
            throw new ArtistException("ARTIST_NOT_FOUND", "Artist was not found");
        }
    }

    private FollowState state(int artistId, int userId) throws SQLException {
        return new FollowState(followRepository.isFollowing(artistId, userId),
                followRepository.countFollowers(artistId));
    }

    /** The follow state of an artist for the current user. */
    public record FollowState(boolean following, long followerCount) {}
}