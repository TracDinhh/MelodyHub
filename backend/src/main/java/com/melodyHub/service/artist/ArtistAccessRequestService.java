package com.melodyHub.service.artist;

import com.melodyHub.dto.request.ArtistAccessRequestCreateRequest;
import com.melodyHub.dto.response.ArtistAccessRequestResponse;
import com.melodyHub.entity.ArtistAccessRequest;
import com.melodyHub.entity.ArtistAccessRequestType;
import com.melodyHub.entity.ArtistRelationship;
import com.melodyHub.entity.User;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistAccessRequestRepository;
import com.melodyHub.repository.ArtistMemberRepository;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;

/**
 * Handles submission and retrieval of artist access requests by authenticated users.
 *
 * <p>Enforces all validation invariants before persisting:</p>
 *
 * <h2>CLAIM_ARTIST invariants</h2>
 * <ul>
 *   <li>{@code artistId} must not be null and must reference an existing, non-deleted artist</li>
 *   <li>User must not already have an active membership for that artist</li>
 *   <li>User must not have a pending CLAIM for the same artist</li>
 *   <li>{@code requestedArtistName} must be null/blank</li>
 * </ul>
 *
 * <h2>CREATE_ARTIST invariants (MVP)</h2>
 * <ul>
 *   <li>{@code artistId} must be null</li>
 *   <li>{@code requestedArtistName} must not be null or blank</li>
 *   <li>{@code relationship} must be {@link ArtistRelationship#ARTIST}
 *       (MVP restriction — managers creating artists is a future feature)</li>
 * </ul>
 */
public class ArtistAccessRequestService {
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_BIO_LENGTH = 16_000;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_MESSAGE_LENGTH = 2_000;

    private final AuthorizationService authorizationService;
    private final ArtistAccessRequestRepository accessRequestRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistRepository artistRepository;

    public ArtistAccessRequestService() {
        this(
                new AuthorizationService(),
                new ArtistAccessRequestRepository(),
                new ArtistMemberRepository(),
                new ArtistRepository()
        );
    }

    public ArtistAccessRequestService(
            AuthorizationService authorizationService,
            ArtistAccessRequestRepository accessRequestRepository,
            ArtistMemberRepository artistMemberRepository,
            ArtistRepository artistRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.accessRequestRepository = Objects.requireNonNull(
                accessRequestRepository, "accessRequestRepository must not be null");
        this.artistMemberRepository = Objects.requireNonNull(
                artistMemberRepository, "artistMemberRepository must not be null");
        this.artistRepository = Objects.requireNonNull(
                artistRepository, "artistRepository must not be null");
    }

    // ─── Submit ──────────────────────────────────────────────────────────────

    /**
     * Submits a new PENDING artist access request.
     *
     * @param token   JWT bearer token
     * @param request parsed request body
     * @return the created request as a user-facing response
     */
    public ArtistAccessRequestResponse submit(String token, ArtistAccessRequestCreateRequest request)
            throws AuthException, ArtistException, SQLException {
        User user = authorizationService.requireAuthenticated(token);

        if (request == null || request.getRequestType() == null) {
            throw new ArtistException("INVALID_REQUEST", "Request body and requestType are required");
        }

        ArtistAccessRequest created = switch (request.getRequestType()) {
            case CLAIM_ARTIST -> submitClaim(user, request);
            case CREATE_ARTIST -> submitCreate(user, request);
        };

        return ArtistAccessRequestResponse.fromEntity(created);
    }

    // ─── My requests ─────────────────────────────────────────────────────────

    /**
     * Returns all access requests submitted by the authenticated user, newest first.
     */
    public List<ArtistAccessRequestResponse> getMyRequests(String token)
            throws AuthException, SQLException {
        User user = authorizationService.requireAuthenticated(token);
        List<ArtistAccessRequest> requests = accessRequestRepository.findAllByUserId(user.getId());
        return requests.stream()
                .map(ArtistAccessRequestResponse::fromEntity)
                .toList();
    }

    // ─── Private: CLAIM ──────────────────────────────────────────────────────

    private ArtistAccessRequest submitClaim(User user, ArtistAccessRequestCreateRequest request)
            throws ArtistException, SQLException {
        Integer artistId = request.getArtistId();

        // Invariant: artistId required
        if (artistId == null) {
            throw new ArtistException(
                    "CLAIM_ARTIST_ID_REQUIRED",
                    "artistId is required for CLAIM_ARTIST requests"
            );
        }

        // Invariant: requestedArtistName must be absent
        if (request.getRequestedArtistName() != null && !request.getRequestedArtistName().isBlank()) {
            throw new ArtistException(
                    "CLAIM_ARTIST_NAME_NOT_ALLOWED",
                    "requestedArtistName must not be provided for CLAIM_ARTIST requests"
            );
        }

        // Invariant: artist must exist and not be deleted
        if (!artistRepository.existsActiveById(artistId)) {
            throw new ArtistException(
                    "ARTIST_NOT_FOUND",
                    "The artist you are trying to claim does not exist or has been removed"
            );
        }

        // Invariant: user must not already be a member of this artist
        if (artistMemberRepository.existsByUserAndArtist(user.getId(), artistId)) {
            throw new ArtistException(
                    "ALREADY_A_MEMBER",
                    "You are already a member of this artist"
            );
        }

        // Invariant: no duplicate pending CLAIM for the same artist
        if (accessRequestRepository.existsPendingClaimByUserAndArtist(user.getId(), artistId)) {
            throw new ArtistException(
                    "CLAIM_REQUEST_ALREADY_PENDING",
                    "You already have a pending claim request for this artist"
            );
        }

        ArtistRelationship relationship = resolveRelationship(request.getRelationship());

        return accessRequestRepository.create(
                artistId,
                user.getId(),
                ArtistAccessRequestType.CLAIM_ARTIST,
                null,                               // no requestedArtistName for CLAIM
                null,
                null,
                relationship,
                normalizeOptional(request.getWebsiteUrl()),
                normalizeOptional(request.getSocialUrl()),
                normalizeMessage(request.getMessage())
        );
    }

    // ─── Private: CREATE ─────────────────────────────────────────────────────

    private ArtistAccessRequest submitCreate(User user, ArtistAccessRequestCreateRequest request)
            throws ArtistException, SQLException {
        // MVP restriction: only ARTIST relationship allowed for CREATE_ARTIST
        ArtistRelationship relationship = resolveRelationship(request.getRelationship());
        if (relationship != ArtistRelationship.ARTIST) {
            throw new ArtistException(
                    "CREATE_ARTIST_REQUIRES_ARTIST_RELATIONSHIP",
                    "Only users with relationship=ARTIST can submit a CREATE_ARTIST request. "
                    + "Future: labels and managers will be able to create artists independently."
            );
        }

        // Invariant: artistId must be absent
        if (request.getArtistId() != null) {
            throw new ArtistException(
                    "CREATE_ARTIST_ID_NOT_ALLOWED",
                    "artistId must not be provided for CREATE_ARTIST requests"
            );
        }

        // Invariant: requestedArtistName required
        String name = request.getRequestedArtistName();
        if (name == null || name.isBlank()) {
            throw new ArtistException(
                    "ARTIST_NAME_REQUIRED",
                    "requestedArtistName is required for CREATE_ARTIST requests"
            );
        }
        name = name.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ArtistException(
                    "ARTIST_NAME_TOO_LONG",
                    "Artist name must be " + MAX_NAME_LENGTH + " characters or less"
            );
        }

        String bio = normalizeOptional(request.getRequestedBio());
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new ArtistException(
                    "ARTIST_BIO_TOO_LONG",
                    "Bio must be " + MAX_BIO_LENGTH + " characters or less"
            );
        }

        String imageUrl = normalizeOptional(request.getRequestedImageUrl());
        if (imageUrl != null && (imageUrl.length() > MAX_URL_LENGTH || !isHttpUrl(imageUrl))) {
            throw new ArtistException(
                    "INVALID_ARTIST_IMAGE_URL",
                    "Image URL must be a valid HTTP/HTTPS URL of 500 characters or less"
            );
        }

        return accessRequestRepository.create(
                null,                               // no artistId for CREATE
                user.getId(),
                ArtistAccessRequestType.CREATE_ARTIST,
                name,
                bio,
                imageUrl,
                ArtistRelationship.ARTIST,
                normalizeOptional(request.getWebsiteUrl()),
                normalizeOptional(request.getSocialUrl()),
                normalizeMessage(request.getMessage())
        );
    }

    // ─── Slug generation (used by Admin approval service, exposed here for reuse) ─────

    /**
     * Generates a unique URL-safe slug from a given artist name.
     * Used by {@code AdminArtistAccessRequestService} during CREATE_ARTIST approval.
     */
    public String generateUniqueSlug(String name) throws SQLException {
        String base = slugify(name);
        if (base.isBlank()) {
            base = "artist";
        }
        if (base.length() > 214) { // 220 max - 6 for "-NNNNN" suffix
            base = base.substring(0, 214);
        }

        String candidate = base;
        int suffix = 2;
        while (artistRepository.slugExists(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ArtistRelationship resolveRelationship(ArtistRelationship relationship) {
        return relationship != null ? relationship : ArtistRelationship.ARTIST;
    }

    private String slugify(String input) {
        String lowered = input.toLowerCase().replace('đ', 'd');
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeMessage(String value) throws ArtistException {
        String trimmed = normalizeOptional(value);
        if (trimmed != null && trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new ArtistException(
                    "MESSAGE_TOO_LONG",
                    "Message must be " + MAX_MESSAGE_LENGTH + " characters or less"
            );
        }
        return trimmed;
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
