package com.melodyHub.controller.artist;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ArtistAccessRequestCreateRequest;
import com.melodyHub.entity.ArtistAccessRequestStatus;
import com.melodyHub.exception.ArtistException;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.artist.ArtistAccessRequestService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Artist access requests (CLAIM_ARTIST / CREATE_ARTIST). Mapped to /api/artist-access-requests/*.
 *
 * <p>Endpoints:</p>
 * <pre>
 * POST /api/artist-access-requests        — submit a new request (PENDING)
 * GET  /api/artist-access-requests/me     — the current user's request history
 * </pre>
 *
 * <p>These are public-user endpoints: any authenticated USER may submit a
 * request. Admin review happens through {@code /api/admin/artist-access-requests}.</p>
 */
public class ArtistAccessRequestServlet extends JsonServlet {
    private ArtistAccessRequestService artistAccessRequestService;

    @Override
    public void init() throws ServletException {
        artistAccessRequestService = new ArtistAccessRequestService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            String path = getPath(request);
            if ("/".equals(path) || "".equals(path)) {
                ArtistAccessRequestCreateRequest body = objectMapper.readValue(
                        request.getInputStream(),
                        ArtistAccessRequestCreateRequest.class
                );
                writeJson(
                        response,
                        HttpServletResponse.SC_CREATED,
                        artistAccessRequestService.submit(getBearerToken(request), body)
                );
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint was not found");
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (ArtistException exception) {
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
            String path = getPath(request);
            if ("/me".equals(path)) {
                writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        artistAccessRequestService.getMyRequests(getBearerToken(request))
                );
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint was not found");
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

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private int getStatusCode(ArtistException exception) {
        return switch (exception.getCode()) {
            case "ARTIST_NOT_FOUND", "REQUEST_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            case "ALREADY_A_MEMBER",
                    "CLAIM_REQUEST_ALREADY_PENDING",
                    "CREATE_ARTIST_REQUIRES_ARTIST_RELATIONSHIP" -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}