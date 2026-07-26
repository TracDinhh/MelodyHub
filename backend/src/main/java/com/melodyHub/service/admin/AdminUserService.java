package com.melodyHub.service.admin;

import com.melodyHub.dto.response.ArtistAdminResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.UserResponse;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Admin-only read access to the user and artist directories.
 */
public class AdminUserService {
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    public AdminUserService() {
        this(new AuthorizationService(), new UserRepository(), new ArtistRepository());
    }

    public AdminUserService(
            AuthorizationService authorizationService,
            UserRepository userRepository,
            ArtistRepository artistRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
    }

    public PagedResponse<UserResponse> listUsers(
            String token,
            UserRole role,
            String query,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int offset = (page - 1) * size;
        List<UserResponse> items = userRepository.findPage(role, query, size, offset)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
        long total = userRepository.countUsers(role, query);
        return new PagedResponse<>(items, total, page, size);
    }

    public PagedResponse<ArtistAdminResponse> listArtists(
            String token,
            String query,
            int page,
            int size
    ) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        int offset = (page - 1) * size;
        List<ArtistAdminResponse> items = artistRepository.findPage(query, size, offset)
                .stream()
                .map(ArtistAdminResponse::fromRow)
                .toList();
        long total = artistRepository.count(query);
        return new PagedResponse<>(items, total, page, size);
    }
}
