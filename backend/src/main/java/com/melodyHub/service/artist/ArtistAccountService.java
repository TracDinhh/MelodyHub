package com.melodyHub.service.artist;

import com.melodyHub.dto.request.ArtistProfileUpdateRequest;
import com.melodyHub.dto.response.ArtistProfileResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.exception.AuthException;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.repository.ArtistRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

public class ArtistAccountService {
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_SLUG_LENGTH = 220;
    private static final int MAX_BIO_LENGTH = 16_000;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final ArtistRepository artistRepository;

    public ArtistAccountService() {
        this(new ArtistRepository());
    }

    public ArtistAccountService(ArtistRepository artistRepository) {
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
    }

    /**
     * Returns the profile for a specific artist. Membership is verified by the
     * caller via {@link ArtistAuthorizationService}. Used by Studio endpoints.
     */
    public ArtistProfileResponse getArtistProfile(int artistId) throws ArtistException, SQLException {
        return ArtistProfileResponse.fromEntity(getArtistEntity(artistId));
    }

    /**
     * Resolves the active {@link Artist} entity by id, or throws when the artist
     * does not exist or has been soft-deleted. Membership is verified by the
     * caller. Used by Studio endpoints.
     */
    public Artist getArtistEntity(int artistId) throws ArtistException, SQLException {
        return artistRepository.findActiveById(artistId)
                .orElseThrow(() -> new ArtistException(
                        "ARTIST_NOT_FOUND",
                        "Artist was not found"
                ));
    }

    public ArtistProfileResponse updateCurrentArtistProfile(Artist artist, ArtistProfileUpdateRequest request)
            throws AuthException, ArtistException, SQLException {
        Objects.requireNonNull(artist, "artist must not be null");
        validateUpdateRequest(request);

        String name = request.getName().trim();
        String slug = request.getSlug().trim();
        String bio = normalizeOptional(request.getBio());
        String imageUrl = normalizeOptional(request.getImageUrl());

        try {
            Artist updatedArtist = artistRepository.updateProfile(
                            artist.getId(),
                            name,
                            slug,
                            bio,
                            imageUrl
                    )
                    .orElseThrow(() -> new AuthException(
                            "ARTIST_PROFILE_NOT_FOUND",
                            "Artist profile was not found"
                    ));
            return ArtistProfileResponse.fromEntity(updatedArtist);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE) {
                throw new ArtistException("ARTIST_SLUG_EXISTS", "Artist slug already exists");
            }
            throw exception;
        }
    }

    private void validateUpdateRequest(ArtistProfileUpdateRequest request) throws ArtistException {
        if (request == null) {
            throw new ArtistException("INVALID_REQUEST", "Artist profile update request is required");
        }

        String name = request.getName();
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
                    "Artist slug must be a lowercase URL slug of 220 characters or less"
            );
        }

        String bio = normalizeOptional(request.getBio());
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new ArtistException(
                    "INVALID_ARTIST_BIO",
                    "Artist bio must be 16000 characters or less"
            );
        }

        String imageUrl = normalizeOptional(request.getImageUrl());
        if (imageUrl != null && (imageUrl.length() > MAX_IMAGE_URL_LENGTH || !isHttpUrl(imageUrl))) {
            throw new ArtistException(
                    "INVALID_ARTIST_IMAGE_URL",
                    "Artist image URL must be an HTTP or HTTPS URL of 500 characters or less"
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
