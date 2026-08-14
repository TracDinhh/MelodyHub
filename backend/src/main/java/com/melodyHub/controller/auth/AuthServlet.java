package com.melodyHub.controller.auth;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ForgotPasswordRequest;
import com.melodyHub.dto.request.LoginRequest;
import com.melodyHub.dto.request.RefreshTokenRequest;
import com.melodyHub.dto.request.RegisterRequest;
import com.melodyHub.dto.request.ResetPasswordRequest;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.auth.AuthService;
import com.melodyHub.service.auth.PasswordResetService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class AuthServlet extends JsonServlet {
    private AuthService authService;
    private PasswordResetService passwordResetService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
        passwordResetService = new PasswordResetService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            switch (getPath(request)) {
                case "/register" -> handleRegister(request, response);
                case "/login" -> handleLogin(request, response);
                case "/refresh" -> handleRefresh(request, response);
                case "/logout" -> handleLogout(request, response);
                case "/forgot-password" -> handleForgotPassword(request, response);
                case "/reset-password" -> handleResetPassword(request, response);
                default -> writeError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "NOT_FOUND",
                        "Auth endpoint was not found"
                );
            }
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        } catch (IOException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_JSON",
                    "Request body is invalid"
            );
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if ("/me".equals(getPath(request))) {
                handleMe(request, response);
                return;
            }

            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "Auth endpoint was not found"
            );
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        RegisterRequest registerRequest = objectMapper.readValue(request.getInputStream(), RegisterRequest.class);
        writeJson(response, HttpServletResponse.SC_CREATED, authService.register(registerRequest));
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        writeJson(response, HttpServletResponse.SC_OK, authService.login(loginRequest));
    }

    private void handleRefresh(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        RefreshTokenRequest refreshTokenRequest = objectMapper.readValue(
                request.getInputStream(),
                RefreshTokenRequest.class
        );
        writeJson(response, HttpServletResponse.SC_OK, authService.refresh(refreshTokenRequest));
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        if (request.getContentLengthLong() > 0) {
            RefreshTokenRequest refreshTokenRequest = objectMapper.readValue(
                    request.getInputStream(),
                    RefreshTokenRequest.class
            );
            authService.logout(refreshTokenRequest);
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void handleMe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        writeJson(response, HttpServletResponse.SC_OK, authService.getCurrentUser(getBearerToken(request)));
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        ForgotPasswordRequest forgotRequest = objectMapper.readValue(
                request.getInputStream(),
                ForgotPasswordRequest.class
        );
        passwordResetService.requestReset(forgotRequest.email());
        // Always return 200 OK to prevent email enumeration
        writeJson(response, HttpServletResponse.SC_OK, java.util.Map.of("message", "If an account exists with that email, a reset link has been sent."));
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        ResetPasswordRequest resetRequest = objectMapper.readValue(
                request.getInputStream(),
                ResetPasswordRequest.class
        );
        passwordResetService.resetPassword(resetRequest.token(), resetRequest.newPassword());
        writeJson(response, HttpServletResponse.SC_OK, java.util.Map.of("message", "Password has been reset successfully."));
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "INVALID_REQUEST",
                    "INVALID_USERNAME",
                    "INVALID_EMAIL",
                    "INVALID_PASSWORD",
                    "INVALID_DISPLAY_NAME" -> HttpServletResponse.SC_BAD_REQUEST;
            case "INVALID_CREDENTIALS",
                    "MISSING_TOKEN",
                    "INVALID_TOKEN",
                    "INVALID_REFRESH_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
            case "FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            case "USERNAME_EXISTS",
                    "EMAIL_EXISTS" -> HttpServletResponse.SC_CONFLICT;
            case "USER_NOT_FOUND",
                    "ARTIST_PROFILE_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}
