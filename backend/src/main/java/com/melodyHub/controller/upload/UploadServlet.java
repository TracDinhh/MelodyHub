package com.melodyHub.controller.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.dto.response.ErrorResponse;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.storage.ImageStorageException;
import com.melodyHub.service.upload.MediaUploadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class UploadServlet extends HttpServlet {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FILE_PART_NAME = "file";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private MediaUploadService mediaUploadService;

    @Override
    public void init() throws ServletException {
        mediaUploadService = new MediaUploadService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!"/image".equals(getPath(request))) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Upload endpoint was not found");
                return;
            }

            Part filePart = getFilePart(request);
            if (filePart == null) {
                writeError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "MISSING_FILE",
                        "A file part named 'file' is required"
                );
                return;
            }

            byte[] content = readAllBytes(filePart);
            var uploadResponse = mediaUploadService.uploadImageForCurrentUser(
                    getBearerToken(request),
                    content,
                    filePart.getSubmittedFileName(),
                    filePart.getContentType(),
                    filePart.getSize()
            );

            writeJson(response, HttpServletResponse.SC_CREATED, uploadResponse);
        } catch (AuthException exception) {
            writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_FILE", exception.getMessage());
        } catch (ImageStorageException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_GATEWAY,
                    "UPLOAD_FAILED",
                    "Image upload failed. Please try again."
            );
        } catch (SQLException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR",
                    "Database error occurred"
            );
        } catch (ServletException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_MULTIPART",
                    "Request must be multipart/form-data"
            );
        }
    }

    private Part getFilePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            throw new ServletException("Not a multipart request");
        }

        return request.getPart(FILE_PART_NAME);
    }

    private byte[] readAllBytes(Part filePart) throws IOException {
        try (InputStream inputStream = filePart.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private String getPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
    }

    private String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private int getStatusCode(AuthException exception) {
        return switch (exception.getCode()) {
            case "MISSING_TOKEN", "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "USER_BANNED", "FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

    private void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
        response.setStatus(statusCode);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(CONTENT_TYPE_JSON);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeError(HttpServletResponse response, int statusCode, String code, String message)
            throws IOException {
        writeJson(response, statusCode, new ErrorResponse(code, message));
    }
}
