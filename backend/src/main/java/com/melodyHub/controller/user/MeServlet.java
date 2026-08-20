package com.melodyHub.controller.user;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.response.MyArtistResponse;
import com.melodyHub.entity.ArtistMember;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.service.artist.ArtistAuthorizationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Authenticated self-service endpoints. Mapped to /api/me/*.
 *
 * <p>Returns data about the current user's account rather than about an
 * arbitrary resource. Unlike resource-scoped APIs, there is nothing to guess
 * or enumerate here — every response is derived from the bearer token.</p>
 */
public class MeServlet extends JsonServlet {
    private ArtistAuthorizationService artistAuthorizationService;
    private ArtistRepository artistRepository;

    @Override
    public void init() throws ServletException {
        artistAuthorizationService = new ArtistAuthorizationService();
        artistRepository = new ArtistRepository();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/artists".equals(path)) {
                handleMyArtists(request, response);
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

    /**
     * Returns all artists the current user is a member of (membership-based,
     * not role-based). Used by the Studio entry view to decide redirect vs
     * artist selector vs access flow.
     */
    private void handleMyArtists(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AuthException, SQLException {
        List<ArtistMember> members = artistAuthorizationService.getUserMemberships(getBearerToken(request));

        List<MyArtistResponse> artists = new ArrayList<>();
        for (ArtistMember member : members) {
            artistRepository.findActiveById(member.getArtistId())
                    .ifPresent(artist -> artists.add(MyArtistResponse.from(member, artist)));
        }

        writeJson(response, HttpServletResponse.SC_OK, artists);
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }
}