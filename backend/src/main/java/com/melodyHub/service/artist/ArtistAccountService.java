package com.melodyHub.service.artist;

import com.melodyHub.entity.Artist;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.SQLException;
import java.util.Objects;

public class ArtistAccountService {
    private final AuthorizationService authorizationService;
    private final ArtistRepository artistRepository;

    public ArtistAccountService() {
        this(new AuthorizationService(), new ArtistRepository());
    }

    public ArtistAccountService(AuthorizationService authorizationService, ArtistRepository artistRepository) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "authorizationService must not be null"
        );
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
    }

    public Artist getCurrentArtist(String token) throws AuthException, SQLException {
        User user = authorizationService.requireRole(token, UserRole.ARTIST);
        return artistRepository.findActiveByUserId(user.getId())
                .orElseThrow(() -> new AuthException(
                        "ARTIST_PROFILE_NOT_FOUND",
                        "Artist profile was not found"
                ));
    }
}
