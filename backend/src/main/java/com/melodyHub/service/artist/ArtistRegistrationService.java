package com.melodyHub.service.artist;

import com.melodyHub.dto.request.BecomeArtistRequest;
import com.melodyHub.dto.response.ArtistRequestResponse;
import com.melodyHub.entity.ArtistRequest;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.ArtistRequestRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles a listener's request to become an Artist. Submitting no longer upgrades
 * the account immediately — it creates a PENDING request that an Admin reviews.
 */
public class ArtistRegistrationService {
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_SLUG_LENGTH = 220;
    private static final int MAX_BIO_LENGTH = 16_000;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final AuthorizationService authorizationService;
    private final ArtistRequestRepository artistRequestRepository;
    private final ArtistRepository artistRepository;

    public ArtistRegistrationService() {
        this(new AuthorizationService(), new ArtistRequestRepository(), new ArtistRepository());
    }

    public ArtistRegistrationService(
            AuthorizationService authorizationService,
            ArtistRequestRepository artistRequestRepository,
            ArtistRepository artistRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.artistRequestRepository = Objects.requireNonNull(
                artistRequestRepository, "artistRequestRepository must not be null");
        this.artistRepository = Objects.requireNonNull(
                artistRepository, "artistRepository must not be null");
    }

    /**
     * Submits a PENDING artist request for the authenticated listener.
     */
    public ArtistRequestResponse submitRequest(String token, BecomeArtistRequest request)
            throws AuthException, ArtistException, SQLException {
        User user = authorizationService.requireRole(token, UserRole.USER);

        validateRequest(request);

        if (artistRepository.existsActiveByUserId(user.getId())) {
            throw new ArtistException("ARTIST_ALREADY_EXISTS", "This account is already an artist");
        }
        if (artistRequestRepository.existsPendingByUserId(user.getId())) {
            throw new ArtistException("ARTIST_REQUEST_PENDING_EXISTS", "You already have a request under review");
        }

        ArtistRequest created = artistRequestRepository.create(
                user.getId(),
                request.getArtistName().trim(),
                request.getSlug().trim(),
                normalizeOptional(request.getBio()),
                normalizeOptional(request.getImageUrl())
        );
        return ArtistRequestResponse.fromEntity(created);
    }

    /**
     * Returns the authenticated user's latest artist request, or null if none.
     */
    public ArtistRequestResponse getMyRequest(String token) throws AuthException, SQLException {
        User user = authorizationService.requireAuthenticated(token);
        Optional<ArtistRequest> request = artistRequestRepository.findLatestByUserId(user.getId());
        return request.map(ArtistRequestResponse::fromEntity).orElse(null);
    }

    // -------------------------------------------------------------------------

    private void validateRequest(BecomeArtistRequest request) throws ArtistException {
        if (request == null) {
            throw new ArtistException("INVALID_REQUEST", "Request body is required");
        }

        String name = request.getArtistName();
        if (name == null || name.isBlank() || name.trim().length() > MAX_NAME_LENGTH) {
            throw new ArtistException(
                    "INVALID_ARTIST_NAME",
                    "Artist name is required and must be 200 characters or less"
            );
        }

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            throw new ArtistException("INVALID_ARTIST_SLUG", "Artist slug is required");
        }

        String normalizedSlug = slug.trim();
        if (normalizedSlug.length() > MAX_SLUG_LENGTH || !SLUG_PATTERN.matcher(normalizedSlug).matches()) {
            throw new ArtistException(
                    "INVALID_ARTIST_SLUG",
                    "Artist slug must be lowercase letters, numbers, and hyphens only (e.g. my-artist-name)"
            );
        }

        String bio = normalizeOptional(request.getBio());
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new ArtistException("INVALID_ARTIST_BIO", "Bio must be 16000 characters or less");
        }

        String imageUrl = normalizeOptional(request.getImageUrl());
        if (imageUrl != null && (imageUrl.length() > MAX_IMAGE_URL_LENGTH || !isHttpUrl(imageUrl))) {
            throw new ArtistException(
                    "INVALID_ARTIST_IMAGE_URL",
                    "Image URL must be a valid HTTP/HTTPS URL of 500 characters or less"
            );
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
