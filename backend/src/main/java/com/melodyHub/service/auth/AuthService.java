package com.melodyHub.service.auth;

import com.melodyHub.dto.request.LoginRequest;
import com.melodyHub.dto.request.ProfileUpdateRequest;
import com.melodyHub.dto.request.RefreshTokenRequest;
import com.melodyHub.dto.request.RegisterRequest;
import com.melodyHub.dto.response.AuthResponse;
import com.melodyHub.dto.response.UserResponse;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserRole;
import com.melodyHub.entity.UserStatus;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.util.JwtUtil;
import com.melodyHub.util.PasswordUtil;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final RefreshTokenService refreshTokenService;

    public AuthService() {
        this(new UserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this(userRepository, new AuthorizationService(userRepository), new RefreshTokenService());
    }

    public AuthService(
            UserRepository userRepository,
            AuthorizationService authorizationService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "authorizationService must not be null"
        );
        this.refreshTokenService = Objects.requireNonNull(
                refreshTokenService,
                "refreshTokenService must not be null"
        );
    }

    public AuthResponse register(RegisterRequest request) throws AuthException, SQLException {
        validateRegisterRequest(request);

        String username = normalize(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String displayName = normalizeOptional(request.getDisplayName());

        if (userRepository.existsByUsername(username)) {
            throw new AuthException("USERNAME_EXISTS", "Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new AuthException("EMAIL_EXISTS", "Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setDisplayName(displayName);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        User createdUser = userRepository.create(user);
        return buildAuthResponse(createdUser);
    }

    public AuthResponse login(LoginRequest request) throws AuthException, SQLException {
        validateLoginRequest(request);

        String usernameOrEmail = normalize(request.getUsernameOrEmail());
        User user = findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new AuthException(
                        "INVALID_CREDENTIALS",
                        "Username/email or password is incorrect"
                ));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthException("USER_BANNED", "User account is banned");
        }

        if (!PasswordUtil.verify(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "Username/email or password is incorrect");
        }

        return buildAuthResponse(user);
    }

    public UserResponse getCurrentUser(String token) throws AuthException, SQLException {
        return UserResponse.fromEntity(authorizationService.requireAuthenticated(token));
    }

    public UserResponse updateMyProfile(String token, ProfileUpdateRequest request) throws AuthException, SQLException {
        if (request == null) {
            throw new AuthException("INVALID_REQUEST", "Profile update request is required");
        }

        User current = authorizationService.requireAuthenticated(token);
        String displayName = normalizeOptional(request.getDisplayName());
        String email = normalizeEmail(request.getEmail());
        String phone = normalizeOptional(request.getPhone());
        String avatarUrl = normalizeOptional(request.getAvatarUrl());

        if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new AuthException("INVALID_DISPLAY_NAME", "Display name must be 100 characters or less");
        }
        if (email == null || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new AuthException("INVALID_EMAIL", "Email is invalid");
        }
        if (avatarUrl != null && avatarUrl.length() > 500) {
            throw new AuthException("INVALID_AVATAR_URL", "Avatar URL must be 500 characters or less");
        }

        if (!email.equals(current.getEmail()) && userRepository.existsByEmail(email)) {
            throw new AuthException("EMAIL_EXISTS", "Email already exists");
        }

        User updated = userRepository.updateProfile(current.getId(), displayName, phone, email, avatarUrl)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User was not found"));

        return UserResponse.fromEntity(updated);
    }

    public AuthResponse refresh(RefreshTokenRequest request) throws AuthException, SQLException {
        String refreshToken = normalize(request == null ? null : request.getRefreshToken());
        if (refreshToken == null) {
            throw new AuthException("INVALID_REFRESH_TOKEN", "Refresh token is required");
        }

        int userId = refreshTokenService.findActiveUserId(refreshToken)
                .orElseThrow(() -> new AuthException(
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or expired"
                ));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User was not found"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthException("USER_BANNED", "User account is banned");
        }

        refreshTokenService.revoke(refreshToken);
        return buildAuthResponse(user);
    }

    public void logout(RefreshTokenRequest request) throws SQLException {
        if (request == null) {
            return;
        }

        refreshTokenService.revoke(request.getRefreshToken());
    }

    private AuthResponse buildAuthResponse(User user) throws SQLException {
        var issuedRefreshToken = refreshTokenService.issue(user.getId());
        return new AuthResponse(
                UserResponse.fromEntity(user),
                JwtUtil.generateToken(user),
                TOKEN_TYPE,
                JwtUtil.getExpiresInSeconds(),
                issuedRefreshToken.token(),
                issuedRefreshToken.expiresInSeconds()
        );
    }

    private Optional<User> findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        Optional<User> userByUsername = userRepository.findByUsername(usernameOrEmail);
        if (userByUsername.isPresent()) {
            return userByUsername;
        }

        return userRepository.findByEmail(normalizeEmail(usernameOrEmail));
    }

    private void validateRegisterRequest(RegisterRequest request) throws AuthException {
        if (request == null) {
            throw new AuthException("INVALID_REQUEST", "Register request is required");
        }

        String username = normalize(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();
        String displayName = normalizeOptional(request.getDisplayName());

        if (username == null) {
            throw new AuthException("INVALID_USERNAME", "Username is required");
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            throw new AuthException("INVALID_USERNAME", "Username must be 3-50 characters");
        }
        if (email == null || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new AuthException("INVALID_EMAIL", "Email is invalid");
        }
        if (password == null || password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("INVALID_PASSWORD", "Password must be at least 6 characters");
        }
        if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new AuthException("INVALID_DISPLAY_NAME", "Display name must be 100 characters or less");
        }
    }

    private void validateLoginRequest(LoginRequest request) throws AuthException {
        if (request == null) {
            throw new AuthException("INVALID_REQUEST", "Login request is required");
        }

        if (normalize(request.getUsernameOrEmail()) == null) {
            throw new AuthException("INVALID_CREDENTIALS", "Username/email and password are required");
        }

        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            throw new AuthException("INVALID_CREDENTIALS", "Username/email and password are required");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        return normalize(value);
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
