package com.melodyHub.service.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.entity.UserStatus;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.util.JwtUtil;
import java.sql.SQLException;
import java.util.Objects;

public class AuthorizationService {
    private final UserRepository userRepository;

    public AuthorizationService() {
        this(new UserRepository());
    }

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public User requireAuthenticated(String token) throws AuthException, SQLException {
        String safeToken = normalize(token);
        if (safeToken == null) {
            throw new AuthException("MISSING_TOKEN", "Token is required");
        }

        int userId;
        try {
            userId = JwtUtil.getUserIdFromToken(safeToken);
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            throw new AuthException("INVALID_TOKEN", "Token is invalid or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User was not found"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthException("USER_BANNED", "User account is banned");
        }

        return user;
    }

    public User requireRole(String token, UserRole requiredRole) throws AuthException, SQLException {
        User user = requireAuthenticated(token);

        if (user.getRole() != requiredRole) {
            throw new AuthException("FORBIDDEN", "User does not have permission to access this resource");
        }

        return user;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
