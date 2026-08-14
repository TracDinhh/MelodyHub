package com.melodyHub.controller.user;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ProfileUpdateRequest;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Self-service user profile. Mapped to /api/users/*.
 */
public class UserServlet extends JsonServlet {
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("PATCH".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
            return;
        }
        super.service(request, response);
    }

    private void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            if ("/me".equals(getPath(request))) {
                handleUpdateMe(request, response);
                return;
            }
            writeError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "NOT_FOUND",
                    "User endpoint was not found"
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
        } catch (IOException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_JSON",
                    "Request body is invalid"
            );
        }
    }

    private void handleUpdateMe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        ProfileUpdateRequest body = objectMapper.readValue(request.getInputStream(), ProfileUpdateRequest.class);
        writeJson(response, HttpServletResponse.SC_OK, authService.updateMyProfile(getBearerToken(request), body));
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "INVALID_REQUEST",
                    "INVALID_DISPLAY_NAME",
                    "INVALID_EMAIL",
                    "INVALID_AVATAR_URL" -> HttpServletResponse.SC_BAD_REQUEST;
            case "MISSING_TOKEN",
                    "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
            case "EMAIL_EXISTS" -> HttpServletResponse.SC_CONFLICT;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}
