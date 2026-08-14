package com.melodyHub.service.admin;

import com.melodyHub.dto.response.ArtistRequestAdminResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.ArtistRequest;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.ArtistRequestRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.service.auth.AuthorizationService;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Admin-only operations over the artist request queue.
 */
public class AdminArtistRequestService {
    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final int MAX_REVIEW_NOTE_LENGTH = 500;

    private final AuthorizationService authorizationService;
    private final ArtistRequestRepository artistRequestRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    public AdminArtistRequestService() {
        this(
                new AuthorizationService(),
                new ArtistRequestRepository(),
                new UserRepository(),
                new ArtistRepository()
        );
    }

    public AdminArtistRequestService(
            AuthorizationService authorizationService,
            ArtistRequestRepository artistRequestRepository,
            UserRepository userRepository,
            ArtistRepository artistRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.artistRequestRepository = Objects.requireNonNull(
                artistRequestRepository, "artistRequestRepository must not be null");
        this.userRepository = Objects.requireNonNull(
                userRepository, "userRepository must not be null");
        this.artistRepository = Objects.requireNonNull(
                artistRepository, "artistRepository must not be null");
    }

    public PagedResponse<ArtistRequestAdminResponse> listRequests(
            String token,
            ArtistRequestStatus status,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int offset = Pagination.offset(page, size);
        List<ArtistRequestAdminResponse> items = artistRequestRepository.findPageByStatus(status, size, offset)
                .stream()
                .map(ArtistRequestAdminResponse::fromRow)
                .toList();
        long total = artistRequestRepository.countByStatus(status);
        return new PagedResponse<>(items, total, page, size);
    }

    /**
     * Approves a PENDING request: promotes the user to ARTIST and creates their
     * artist profile from the request data.
     */
    public ArtistRequestAdminResponse approve(String token, int requestId)
            throws AuthException, ArtistException, SQLException {
        User admin = authorizationService.requireRole(token, UserRole.ADMIN);

        ArtistRequest request = artistRequestRepository.findById(requestId)
                .orElseThrow(() -> new ArtistException("ARTIST_REQUEST_NOT_FOUND", "Artist request was not found"));
        if (request.getStatus() != ArtistRequestStatus.PENDING) {
            throw new ArtistException("ARTIST_REQUEST_NOT_PENDING", "Artist request has already been reviewed");
        }
        if (artistRepository.existsActiveByUserId(request.getUserId())) {
            throw new ArtistException("ARTIST_ALREADY_EXISTS", "This user is already an artist");
        }

        // 1) Promote role so the DB trigger permits linking the artist profile.
        userRepository.updateRole(request.getUserId(), UserRole.ARTIST)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "Requesting user was not found"));

        // 2) Create the artist profile from the request.
        try {
            artistRepository.create(
                    request.getUserId(),
                    request.getArtistName(),
                    request.getSlug(),
                    request.getBio(),
                    request.getImageUrl()
            );
        } catch (SQLException exception) {
            // Roll back the role change to keep state consistent.
            userRepository.updateRole(request.getUserId(), UserRole.USER);
            if (exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE) {
                throw new ArtistException("ARTIST_SLUG_EXISTS", "Artist slug is already taken");
            }
            throw exception;
        }

        // 3) Resolve the request.
        artistRequestRepository.markReviewed(requestId, ArtistRequestStatus.APPROVED, admin.getId(), null);
        return buildResponse(requestId);
    }

    public ArtistRequestAdminResponse reject(String token, int requestId, String reviewNote)
            throws AuthException, ArtistException, SQLException {
        User admin = authorizationService.requireRole(token, UserRole.ADMIN);

        String note = normalizeNote(reviewNote);
        var reviewed = artistRequestRepository.markReviewed(
                requestId,
                ArtistRequestStatus.REJECTED,
                admin.getId(),
                note
        );
        if (reviewed.isEmpty()) {
            // Either it does not exist or it is not PENDING anymore.
            if (artistRequestRepository.findById(requestId).isEmpty()) {
                throw new ArtistException("ARTIST_REQUEST_NOT_FOUND", "Artist request was not found");
            }
            throw new ArtistException("ARTIST_REQUEST_NOT_PENDING", "Artist request has already been reviewed");
        }
        return buildResponse(requestId);
    }

    /**
     * Builds the admin response for a reviewed request (entity + requester lookup).
     */
    private ArtistRequestAdminResponse buildResponse(int requestId) throws SQLException {
        ArtistRequest request = artistRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return null;
        }
        User requester = userRepository.findById(request.getUserId()).orElse(null);
        return new ArtistRequestAdminResponse(
                request.getId(),
                request.getUserId(),
                requester == null ? null : requester.getUsername(),
                requester == null ? null : requester.getDisplayName(),
                requester == null ? null : requester.getEmail(),
                request.getArtistName(),
                request.getSlug(),
                request.getBio(),
                request.getImageUrl(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    private String normalizeNote(String note) throws ArtistException {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.length() > MAX_REVIEW_NOTE_LENGTH) {
            throw new ArtistException("INVALID_REVIEW_NOTE", "Review note must be 500 characters or less");
        }
        return trimmed;
    }
}
