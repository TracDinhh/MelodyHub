package com.melodyHub.service.artist;

import com.melodyHub.entity.ArtistMember;
import com.melodyHub.entity.ArtistMemberRole;
import com.melodyHub.entity.ArtistRelationship;
import com.melodyHub.entity.User;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistMemberRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Central authorization service for Artist Studio access.
 *
 * <p>This is the <b>single source of truth</b> for all membership-based
 * authorization logic. No other service or servlet should contain
 * OWNER/MANAGER if-statements or direct calls to {@link ArtistMemberRepository}
 * for access checks.</p>
 *
 * <p>All methods throw {@link AuthException} on failure — callers must not
 * catch and swallow these; servlets translate them to HTTP error responses.</p>
 *
 * <h2>Authorization model</h2>
 * <pre>
 *   User ──(artist_members)──▶ Artist
 *          role = OWNER | MANAGER
 * </pre>
 *
 * <h2>Permission matrix (MVP)</h2>
 * <pre>
 *   requireArtistAccess()      → OWNER or MANAGER
 *   requireCanEditArtist()     → OWNER or MANAGER
 *   requireCanManageMusic()    → OWNER or MANAGER
 *   requireCanManageMembers()  → OWNER only
 * </pre>
 */
public class ArtistAuthorizationService {

    private final AuthorizationService authorizationService;
    private final ArtistMemberRepository artistMemberRepository;

    public ArtistAuthorizationService() {
        this(new AuthorizationService(), new ArtistMemberRepository());
    }

    public ArtistAuthorizationService(
            AuthorizationService authorizationService,
            ArtistMemberRepository artistMemberRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.artistMemberRepository = Objects.requireNonNull(
                artistMemberRepository, "artistMemberRepository must not be null");
    }

    // ─── Public authorization methods ────────────────────────────────────────

    /**
     * Verifies that the authenticated user has ANY membership (OWNER or MANAGER)
     * in the given artist. This is the baseline check for all Studio routes.
     *
     * @param token    JWT bearer token from the request
     * @param artistId the artist being accessed (from URL path)
     * @return the resolved {@link ArtistMember} (includes role for downstream use)
     * @throws AuthException if unauthenticated, banned, or not a member
     */
    public ArtistMember requireArtistAccess(String token, int artistId)
            throws AuthException, SQLException {
        User user = authorizationService.requireAuthenticated(token);
        return requireMembership(user, artistId);
    }

    /**
     * Verifies that the authenticated user can edit the artist profile or manage music.
     * Both OWNER and MANAGER have this permission.
     */
    public ArtistMember requireCanEditArtist(String token, int artistId)
            throws AuthException, SQLException {
        return requireArtistAccess(token, artistId); // both roles can edit
    }

    /**
     * Verifies that the authenticated user can manage songs for this artist.
     * Both OWNER and MANAGER have this permission.
     */
    public ArtistMember requireCanManageMusic(String token, int artistId)
            throws AuthException, SQLException {
        return requireArtistAccess(token, artistId); // both roles can manage music
    }

    /**
     * Verifies that the authenticated user is the OWNER of the artist.
     * Required for team management and ownership-transfer operations.
     *
     * @throws AuthException if not authenticated, not a member, or only MANAGER
     */
    public ArtistMember requireCanManageMembers(String token, int artistId)
            throws AuthException, SQLException {
        User user = authorizationService.requireAuthenticated(token);
        ArtistMember member = requireMembership(user, artistId);
        if (member.getRole() != ArtistMemberRole.OWNER) {
            throw new AuthException(
                    "ARTIST_OWNER_REQUIRED",
                    "Only the artist OWNER can manage team members"
            );
        }
        return member;
    }

    /**
     * Returns all artists the authenticated user is a member of.
     * Used by {@code GET /api/me/artists}.
     *
     * @param token JWT bearer token
     * @return list of memberships, ordered by creation time (oldest first)
     */
    public List<ArtistMember> getUserMemberships(String token)
            throws AuthException, SQLException {
        User user = authorizationService.requireAuthenticated(token);
        return artistMemberRepository.findByUserId(user.getId());
    }

    /**
     * Returns all members of an artist profile.
     * The caller must have already verified OWNER access via {@link #requireCanManageMembers}.
     */
    public List<ArtistMember> getArtistMembers(int artistId) throws SQLException {
        return artistMemberRepository.findByArtistId(artistId);
    }

    // ─── Relationship → member role mapping ──────────────────────────────────

    /**
     * Resolves the appropriate {@link ArtistMemberRole} for a given relationship.
     *
     * <p>Mapping:</p>
     * <pre>
     *   ARTIST      → OWNER
     *   MANAGER     → MANAGER
     *   LABEL       → MANAGER
     *   TEAM_MEMBER → MANAGER
     *   OTHER       → MANAGER
     * </pre>
     *
     * <p>This method is the <b>only</b> place this mapping is defined.
     * It must not be duplicated in {@code AdminArtistAccessRequestService}
     * or any other class.</p>
     */
    public ArtistMemberRole resolveMemberRole(ArtistRelationship relationship) {
        return relationship == ArtistRelationship.ARTIST
                ? ArtistMemberRole.OWNER
                : ArtistMemberRole.MANAGER;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private ArtistMember requireMembership(User user, int artistId)
            throws AuthException, SQLException {
        return artistMemberRepository
                .findByUserAndArtist(user.getId(), artistId)
                .orElseThrow(() -> new AuthException(
                        "ARTIST_ACCESS_DENIED",
                        "You do not have access to this artist"
                ));
    }
}
