package com.melodyHub.service.auth;

import com.melodyHub.config.AppConfig;
import com.melodyHub.entity.PasswordResetToken;
import com.melodyHub.entity.User;
import com.melodyHub.entity.UserStatus;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.PasswordResetTokenRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.service.EmailService;
import com.melodyHub.util.PasswordUtil;
import com.melodyHub.util.TokenHashUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class PasswordResetService {
    private static final int DEFAULT_EXPIRY_MINUTES = 60;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public PasswordResetService() {
        this(new PasswordResetTokenRepository(), new UserRepository());
    }

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "tokenRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    /**
     * Request a password reset. Generates and stores a token.
     * Returns the plain token so the caller can send it via email.
     */
    public String requestReset(String email) throws SQLException, AuthException {
        if (email == null || email.isBlank()) {
            throw new AuthException("INVALID_EMAIL", "Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        // Always return success to prevent email enumeration attacks
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        if (user.getStatus() == UserStatus.BANNED) {
            return null;
        }

        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate a secure random token
        String plainToken = generateSecureToken();
        String tokenHash = hashToken(plainToken);

        int expiryMinutes = AppConfig.getInt("auth.password-reset.expiry-minutes", DEFAULT_EXPIRY_MINUTES);
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setToken(tokenHash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        token.setCreatedAt(LocalDateTime.now());

        tokenRepository.create(token);

        // Send email with the reset link
        EmailService.getInstance().sendPasswordResetEmail(user.getEmail(), plainToken);

        // Return the plain token so caller can send via email
        return plainToken;
    }

    /**
     * Reset password using a valid token.
     */
    public void resetPassword(String plainToken, String newPassword) throws SQLException, AuthException {
        if (plainToken == null || plainToken.isBlank()) {
            throw new AuthException("INVALID_TOKEN", "Reset token is required");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new AuthException("INVALID_PASSWORD", "Password must be at least 6 characters");
        }

        String tokenHash = hashToken(plainToken);
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new AuthException("INVALID_TOKEN", "Reset token is invalid or has expired");
        }

        PasswordResetToken token = tokenOpt.get();
        Optional<User> userOpt = userRepository.findById(token.getUserId());

        if (userOpt.isEmpty()) {
            throw new AuthException("USER_NOT_FOUND", "User not found");
        }

        User user = userOpt.get();
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthException("USER_BANNED", "Account is suspended");
        }

        // Update password
        String newHash = PasswordUtil.hash(newPassword);
        userRepository.updatePassword(user.getId(), newHash);

        // Mark token as used
        tokenRepository.markUsed(token.getId());

        // Delete all existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());
    }

    private String generateSecureToken() {
        return TokenHashUtil.randomToken(32);
    }

    private String hashToken(String token) {
        return TokenHashUtil.sha256Hex(token);
    }
}
