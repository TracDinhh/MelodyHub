package com.melodyHub.service.admin;

import com.melodyHub.config.DatabaseConfig;
import com.melodyHub.dto.response.ArtistAccessRequestAdminResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.ArtistAccessRequest;
import com.melodyHub.entity.ArtistAccessRequestStatus;
import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistMemberRole;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistAccessRequestRepository;
import com.melodyHub.repository.ArtistMemberRepository;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.service.artist.ArtistAccessRequestService;
import com.melodyHub.service.artist.ArtistAuthorizationService;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Admin service for reviewing artist access requests.
 *
 * <p>Both APPROVE and REJECT operate within a single database transaction,
 * using {@code FOR UPDATE} locking to prevent double-processing in concurrent requests.</p>
 *
 * <h2>CREATE_ARTIST approval transaction</h2>
 * <ol>
 *   <li>Lock request row FOR UPDATE</li>
 *   <li>Verify status = PENDING</li>
 *   <li>Generate unique slug from requested name</li>
 *   <li>INSERT into artists (no user_id)</li>
 *   <li>INSERT into artist_members with role=OWNER</li>
 *   <li>UPDATE request status = APPROVED</li>
 *   <li>COMMIT</li>
 * </ol>
 *
 * <h2>CLAIM_ARTIST approval transaction</h2>
 * <ol>
 *   <li>Lock request row FOR UPDATE</li>
 *   <li>Verify status = PENDING</li>
 *   <li>Verify artist is still active (not deleted)</li>
 *   <li>Verify no existing membership for (user, artist)</li>
 *   <li>Resolve relationship → member role via {@link ArtistAuthorizationService#resolveMemberRole}</li>
 *   <li>INSERT into artist_members</li>
 *   <li>UPDATE request status = APPROVED</li>
 *   <li>COMMIT</li>
 * </ol>
 */
public class AdminArtistAccessRequestService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final AuthorizationService authorizationService;
    private final ArtistAuthorizationService artistAuthorizationService;
    private final ArtistAccessRequestRepository accessRequestRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAccessRequestService accessRequestService;

    public AdminArtistAccessRequestService() {
        this(
                new AuthorizationService(),
                new ArtistAuthorizationService(),
                new ArtistAccessRequestRepository(),
                new ArtistMemberRepository(),
                new ArtistRepository(),
                new ArtistAccessRequestService()
        );
    }

    public AdminArtistAccessRequestService(
            AuthorizationService authorizationService,
            ArtistAuthorizationService artistAuthorizationService,
            ArtistAccessRequestRepository accessRequestRepository,
            ArtistMemberRepository artistMemberRepository,
            ArtistRepository artistRepository,
            ArtistAccessRequestService accessRequestService
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.artistAuthorizationService = Objects.requireNonNull(
                artistAuthorizationService, "artistAuthorizationService must not be null");
        this.accessRequestRepository = Objects.requireNonNull(
                accessRequestRepository, "accessRequestRepository must not be null");
        this.artistMemberRepository = Objects.requireNonNull(
                artistMemberRepository, "artistMemberRepository must not be null");
        this.artistRepository = Objects.requireNonNull(
                artistRepository, "artistRepository must not be null");
        this.accessRequestService = Objects.requireNonNull(
                accessRequestService, "accessRequestService must not be null");
    }

    // ─── List ────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of requests for the admin review queue.
     *
     * @param token  admin JWT token
     * @param status filter by status (required)
     * @param page   1-based page number
     * @param size   page size (max {@link #MAX_PAGE_SIZE})
     */
    public PagedResponse<ArtistAccessRequestAdminResponse> list(
            String token,
            ArtistAccessRequestStatus status,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        if (page < 1) page = 1;
        if (size < 1) size = DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;

        int offset = (page - 1) * size;
        List<ArtistAccessRequestRepository.AdminRow> rows =
                accessRequestRepository.findPageByStatus(status, size, offset);
        long total = accessRequestRepository.countByStatus(status);

        List<ArtistAccessRequestAdminResponse> items = rows.stream()
                .map(row -> {
                    ArtistMemberRole resolved = artistAuthorizationService
                            .resolveMemberRole(row.request().getRelationship());
                    return ArtistAccessRequestAdminResponse.fromRow(row, resolved);
                })
                .toList();

        return new PagedResponse<>(items, total, page, size);
    }

    // ─── Approve ─────────────────────────────────────────────────────────────

    /**
     * Approves a PENDING access request in a single database transaction.
     * Dispatches to {@link #approveCreate} or {@link #approveClaim} based on request type.
     */
    public ArtistAccessRequestAdminResponse approve(String token, int requestId)
            throws AuthException, ArtistException, SQLException {
        User admin = authorizationService.requireRole(token, UserRole.ADMIN);

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Lock row to prevent double-processing
                ArtistAccessRequest request = accessRequestRepository
                        .findByIdForUpdate(connection, requestId)
                        .orElseThrow(() -> new ArtistException(
                                "REQUEST_NOT_FOUND", "Artist access request was not found"));

                if (request.getStatus() != ArtistAccessRequestStatus.PENDING) {
                    throw new ArtistException(
                            "REQUEST_NOT_PENDING",
                            "This request has already been reviewed"
                    );
                }

                int newArtistId = switch (request.getRequestType()) {
                    case CREATE_ARTIST -> approveCreate(connection, request);
                    case CLAIM_ARTIST -> approveClaim(connection, request);
                };

                accessRequestRepository.markApproved(connection, requestId, admin.getId());
                connection.commit();

                return buildAdminResponse(requestId, newArtistId, request.getRelationship());
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // ─── Reject ──────────────────────────────────────────────────────────────

    /**
     * Rejects a PENDING access request. Also uses a transaction for state consistency.
     */
    public ArtistAccessRequestAdminResponse reject(String token, int requestId, String reviewNote)
            throws AuthException, ArtistException, SQLException {
        User admin = authorizationService.requireRole(token, UserRole.ADMIN);
        String note = normalizeNote(reviewNote);

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ArtistAccessRequest request = accessRequestRepository
                        .findByIdForUpdate(connection, requestId)
                        .orElseThrow(() -> new ArtistException(
                                "REQUEST_NOT_FOUND", "Artist access request was not found"));

                if (request.getStatus() != ArtistAccessRequestStatus.PENDING) {
                    throw new ArtistException(
                            "REQUEST_NOT_PENDING",
                            "This request has already been reviewed"
                    );
                }

                accessRequestRepository.markRejected(connection, requestId, admin.getId(), note);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        // Return full admin view outside transaction
        ArtistAccessRequestRepository.AdminRow row = accessRequestRepository
                .findPageByStatus(ArtistAccessRequestStatus.REJECTED, 1, 0)
                .stream()
                .filter(r -> r.request().getId() == requestId)
                .findFirst()
                .orElse(null);

        if (row != null) {
            ArtistMemberRole resolved = artistAuthorizationService.resolveMemberRole(row.request().getRelationship());
            return ArtistAccessRequestAdminResponse.fromRow(row, resolved);
        }
        return null;
    }

    // ─── Private: CREATE_ARTIST approval ─────────────────────────────────────

    /**
     * Creates a new artist profile and OWNER membership in the same transaction.
     *
     * @return the ID of the newly created artist
     */
    private int approveCreate(Connection connection, ArtistAccessRequest request)
            throws ArtistException, SQLException {
        String name = request.getRequestedArtistName();
        if (name == null || name.isBlank()) {
            throw new ArtistException(
                    "INVALID_REQUEST_DATA",
                    "CREATE_ARTIST request is missing the requested artist name"
            );
        }

        String slug = accessRequestService.generateUniqueSlug(name.trim());

        // Create artist profile (no user_id link)
        var artist = artistRepository.createWithConnection(
                connection,
                name.trim(),
                slug,
                request.getRequestedBio(),
                request.getRequestedImageUrl()
        );

        // Create OWNER membership
        artistMemberRepository.create(connection, artist.getId(), request.getUserId(), ArtistMemberRole.OWNER);

        return artist.getId();
    }

    // ─── Private: CLAIM_ARTIST approval ──────────────────────────────────────

    /**
     * Validates the claim and creates a membership with the resolved role.
     *
     * @return the claimed artist ID
     */
    private int approveClaim(Connection connection, ArtistAccessRequest request)
            throws ArtistException, SQLException {
        int artistId = request.getArtistId();

        // Verify artist still exists and is not deleted
        if (!artistRepository.existsActiveById(artistId)) {
            throw new ArtistException(
                    "ARTIST_NOT_FOUND",
                    "The artist being claimed no longer exists or has been removed"
            );
        }

        // Verify no duplicate membership
        if (artistMemberRepository.existsByUserAndArtist(request.getUserId(), artistId)) {
            throw new ArtistException(
                    "ALREADY_A_MEMBER",
                    "This user is already a member of the artist"
            );
        }

        // Resolve relationship → membership role (centralized in ArtistAuthorizationService)
        ArtistMemberRole role = artistAuthorizationService.resolveMemberRole(request.getRelationship());

        artistMemberRepository.create(connection, artistId, request.getUserId(), role);

        return artistId;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ArtistAccessRequestAdminResponse buildAdminResponse(
            int requestId,
            int artistId,
            com.melodyHub.entity.ArtistRelationship relationship
    ) throws SQLException {
        // Fetch updated request data for response
        List<ArtistAccessRequestRepository.AdminRow> rows =
                accessRequestRepository.findPageByStatus(ArtistAccessRequestStatus.APPROVED, 100, 0);

        for (ArtistAccessRequestRepository.AdminRow row : rows) {
            if (row.request().getId() == requestId) {
                ArtistMemberRole resolved = artistAuthorizationService.resolveMemberRole(relationship);
                return ArtistAccessRequestAdminResponse.fromRow(row, resolved);
            }
        }
        return null;
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
