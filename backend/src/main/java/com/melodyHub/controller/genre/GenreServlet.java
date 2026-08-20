package com.melodyHub.controller.genre;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.service.genre.GenreService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Public genre API. Mapped to {@code /api/genres/*}.
 *
 * <pre>
 * GET /api/genres              → list all genre master data (public)
 * GET /api/genres/{slug}/songs → paged PUBLISHED songs of the genre (public)
 * </pre>
 */
public class GenreServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private GenreService genreService;

    @Override
    public void init() throws ServletException {
        genreService = new GenreService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);
            if ("/".equals(path)) {
                writeJson(response, HttpServletResponse.SC_OK, genreService.listAll());
                return;
            }

            // /genres/{slug}/songs
            if (path.endsWith("/songs")) {
                String slug = path.substring(1, path.length() - "/songs".length());
                if (slug.isBlank() || slug.contains("/")) {
                    writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Genre not found");
                    return;
                }
                int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
                int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
                if (size > MAX_SIZE) {
                    throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
                }
                var result = genreService.getPublishedSongsBySlug(slug, page, size);
                if (result.isEmpty()) {
                    writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Genre not found");
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, result.get());
                return;
            }

            writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Genre endpoint was not found");
        } catch (InvalidQueryParamException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_QUERY_PARAM", e.getMessage());
        } catch (SQLException e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Database error occurred");
        }
    }
}