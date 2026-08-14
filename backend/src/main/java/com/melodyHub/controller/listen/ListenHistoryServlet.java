package com.melodyHub.controller.listen;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.request.ListenHistoryCreateRequest;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.ListenHistoryResponse;
import com.melodyHub.service.listen.ListenHistoryService;
import com.melodyHub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class ListenHistoryServlet extends JsonServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private ListenHistoryService listenHistoryService;

    @Override
    public void init() throws ServletException {
        listenHistoryService = new ListenHistoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        try {
            int page = parsePositiveInt(request.getParameter("page"), "page", DEFAULT_PAGE);
            int size = parsePositiveInt(request.getParameter("size"), "size", DEFAULT_SIZE);
            if (size > MAX_SIZE) {
                throw new InvalidQueryParamException("size must not exceed " + MAX_SIZE);
            }

            PagedResponse<ListenHistoryResponse> payload =
                    listenHistoryService.getPage(userId.get(), page, size);
            writeJson(response, HttpServletResponse.SC_OK, payload);
        } catch (InvalidQueryParamException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_QUERY_PARAM",
                    exception.getMessage()
            );
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        ListenHistoryCreateRequest payload;
        try {
            payload = objectMapper.readValue(request.getInputStream(), ListenHistoryCreateRequest.class);
        } catch (IOException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_JSON",
                    "Request body is not valid JSON"
            );
            return;
        }

        if (payload == null || payload.getSongId() == null) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_PAYLOAD",
                    "songId is required"
            );
            return;
        }

        int playedSec = payload.getPlayedSec() == null ? 0 : payload.getPlayedSec();

        try {
            long historyId = listenHistoryService.record(userId.get(), payload.getSongId(), playedSec);
            writeJson(response, HttpServletResponse.SC_CREATED, Map.of("id", historyId));
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Integer> userId = requireUserId(request);
        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        String path = getPath(request);
        try {
            if ("/".equals(path)) {
                int removed = listenHistoryService.clear(userId.get());
                writeJson(response, HttpServletResponse.SC_OK, Map.of("removed", removed));
                return;
            }

            Long historyId = getHistoryId(path);
            if (historyId == null) {
                writeError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "NOT_FOUND",
                        "Listen history endpoint was not found"
                );
                return;
            }

            boolean removed = listenHistoryService.delete(userId.get(), historyId);
            if (!removed) {
                writeError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "LISTEN_HISTORY_NOT_FOUND",
                        "Listen history entry was not found"
                );
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, Map.of("removed", 1));
        } catch (InvalidQueryParamException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_QUERY_PARAM",
                    exception.getMessage()
            );
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        }
    }

    private Optional<Integer> requireUserId(HttpServletRequest request) {
        String token = getBearerToken(request);
        if (token == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(JwtUtil.getUserIdFromToken(token));
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Long getHistoryId(String path) throws InvalidQueryParamException {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty() || trimmed.contains("/")) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException exception) {
            throw new InvalidQueryParamException("history id must be a positive integer");
        }
    }
}
