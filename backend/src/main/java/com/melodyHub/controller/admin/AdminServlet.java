package com.melodyHub.controller.admin;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ArtistRequestReviewRequest;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.admin.AdminArtistRequestService;
import com.melodyHub.service.admin.AdminStatsService;
import com.melodyHub.service.admin.AdminUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class AdminServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private AdminArtistRequestService adminArtistRequestService;
    private AdminUserService adminUserService;
    private AdminStatsService adminStatsService;

    @Override
    public void init() throws ServletException {
        adminArtistRequestService = new AdminArtistRequestService();
        adminUserService = new AdminUserService();
        adminStatsService = new AdminStatsService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/stats".equals(path)) {
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        adminStatsService.getStats(getBearerToken(request))
                );
                return;
            }
            if ("/artist-requests".equals(path)) {
                handleListRequests(request, response);
                return;
            }
            if ("/users".equals(path)) {
                handleListUsers(request, response);
                return;
            }
            if ("/artists".equals(path)) {
                handleListArtists(request, response);
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Admin endpoint was not found");
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (InvalidQueryParamException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Database error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            String path = getPath(request);
            Integer approveId = matchAction(path, "approve");
            if (approveId != null) {
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        adminArtistRequestService.approve(getBearerToken(request), approveId)
                );
                return;
            }

            Integer rejectId = matchAction(path, "reject");
            if (rejectId != null) {
                String note = readReviewNote(request);
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        adminArtistRequestService.reject(getBearerToken(request), rejectId, note)
                );
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Admin endpoint was not found");
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (ArtistException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (SQLException exception) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Database error occurred");
        }
    }

    private void handleListRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        ArtistRequestStatus status = parseStatus(request.getParameter("status"));
        writeJson(
                response,
                HttpServletResponse.SC_OK,
                adminArtistRequestService.listRequests(getBearerToken(request), status, page, size)
        );
    }

    private void handleListUsers(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        UserRole role = parseRole(request.getParameter("role"));
        String query = request.getParameter("q");
        writeJson(
                response,
                HttpServletResponse.SC_OK,
                adminUserService.listUsers(getBearerToken(request), role, query, page, size)
        );
    }

    private void handleListArtists(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException, InvalidQueryParamException {
        int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
        if (size > MAX_SIZE) {
            throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
        }

        String query = request.getParameter("q");
        writeJson(
                response,
                HttpServletResponse.SC_OK,
                adminUserService.listArtists(getBearerToken(request), query, page, size)
        );
    }

    private UserRole parseRole(String value) throws InvalidQueryParamException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidQueryParamException("role must be USER, ARTIST, or ADMIN");
        }
    }

    private String readReviewNote(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() <= 0) {
            return null;
        }
        try {
            ArtistRequestReviewRequest body = objectMapper.readValue(
                    request.getInputStream(),
                    ArtistRequestReviewRequest.class
            );
            return body == null ? null : body.getReviewNote();
        } catch (IOException exception) {
            return null;
        }
    }

    private ArtistRequestStatus parseStatus(String value) throws InvalidQueryParamException {
        if (value == null || value.isBlank()) {
            return ArtistRequestStatus.PENDING;
        }
        try {
            return ArtistRequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidQueryParamException("status must be PENDING, APPROVED, or REJECTED");
        }
    }

    /**
     * Matches /artist-requests/{id}/{action} and returns the numeric id, or null.
     */
    private Integer matchAction(String path, String action) {
        String prefix = "/artist-requests/";
        String suffix = "/" + action;
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }

        String idPart = path.substring(prefix.length(), path.length() - suffix.length());
        if (idPart.isBlank() || idPart.contains("/")) {
            return null;
        }
        try {
            int id = Integer.parseInt(idPart);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED", "FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private int getStatusCode(ArtistException exception) {
        return switch (exception.getCode()) {
            case "ARTIST_SLUG_EXISTS",
                    "ARTIST_ALREADY_EXISTS",
                    "ARTIST_REQUEST_NOT_PENDING" -> HttpServletResponse.SC_CONFLICT;
            case "ARTIST_REQUEST_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}
