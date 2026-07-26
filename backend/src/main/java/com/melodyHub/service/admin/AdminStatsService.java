package com.melodyHub.service.admin;

import com.melodyHub.dto.response.AdminStatsResponse;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.ArtistRequestRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Aggregates counts for the admin overview dashboard.
 */
public class AdminStatsService {
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistRequestRepository artistRequestRepository;
    private final SongRepository songRepository;

    public AdminStatsService() {
        this(
                new AuthorizationService(),
                new UserRepository(),
                new ArtistRepository(),
                new ArtistRequestRepository(),
                new SongRepository()
        );
    }

    public AdminStatsService(
            AuthorizationService authorizationService,
            UserRepository userRepository,
            ArtistRepository artistRepository,
            ArtistRequestRepository artistRequestRepository,
            SongRepository songRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
        this.artistRequestRepository = Objects.requireNonNull(
                artistRequestRepository, "artistRequestRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public AdminStatsResponse getStats(String token) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        long listeners = userRepository.countUsers(UserRole.USER, null);
        long artists = userRepository.countUsers(UserRole.ARTIST, null);
        long admins = userRepository.countUsers(UserRole.ADMIN, null);

        return new AdminStatsResponse(
                listeners + artists + admins,
                listeners,
                artists,
                admins,
                artistRepository.count(null),
                artistRequestRepository.countByStatus(ArtistRequestStatus.PENDING),
                songRepository.count(null, null)
        );
    }
}
