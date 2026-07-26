package com.melodyHub.service.upload;

import com.melodyHub.dto.response.ImageUploadResponse;
import com.melodyHub.entity.User;
import com.melodyHub.exception.AuthException;
import com.melodyHub.service.auth.AuthorizationService;
import com.melodyHub.service.storage.ImageStorageException;
import com.melodyHub.service.storage.ImageStorageService;
import com.melodyHub.service.storage.imagekit.ImageKitStorageService;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

public class MediaUploadService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_AUDIO_BYTES = 30L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/aac",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/flac"
    );

    private final AuthorizationService authorizationService;
    private final ImageStorageService imageStorageService;

    public MediaUploadService() {
        this(new AuthorizationService(), new ImageKitStorageService());
    }

    public MediaUploadService(
            AuthorizationService authorizationService,
            ImageStorageService imageStorageService
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.imageStorageService = Objects.requireNonNull(
                imageStorageService, "imageStorageService must not be null");
    }

    /**
     * Uploads an image for the authenticated user and returns the stored location.
     * Any signed-in user may upload; the file is stored in the user's own folder.
     */
    public ImageUploadResponse uploadImageForCurrentUser(
            String token,
            byte[] content,
            String fileName,
            String contentType,
            long declaredSize
    ) throws AuthException, ImageStorageException, SQLException {
        User user = authorizationService.requireAuthenticated(token);

        validate(content, contentType, declaredSize);

        var result = imageStorageService.uploadUserImage(user.getId(), content, sanitizeFileName(fileName));
        return ImageUploadResponse.fromResult(result);
    }

    /**
     * Uploads an audio file for the authenticated user and returns the stored location.
     */
    public ImageUploadResponse uploadAudioForCurrentUser(
            String token,
            byte[] content,
            String fileName,
            String contentType,
            long declaredSize
    ) throws AuthException, ImageStorageException, SQLException {
        User user = authorizationService.requireAuthenticated(token);

        validateAudio(content, contentType, declaredSize);

        var result = imageStorageService.uploadUserAudio(user.getId(), content, sanitizeFileName(fileName));
        return ImageUploadResponse.fromResult(result);
    }

    private void validate(byte[] content, String contentType, long declaredSize) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (content.length > MAX_IMAGE_BYTES || declaredSize > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be 5 MB or less");
        }

        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Image must be a JPEG, PNG, or WebP file");
        }
    }

    private void validateAudio(byte[] content, String contentType, long declaredSize) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Audio file is empty");
        }
        if (content.length > MAX_AUDIO_BYTES || declaredSize > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Audio must be 30 MB or less");
        }

        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_AUDIO_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Audio must be an MP3, WAV, AAC, OGG, or FLAC file");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload";
        }

        // Strip any path components a browser might include and keep it filesystem-safe.
        String baseName = fileName.replace('\\', '/');
        int slashIndex = baseName.lastIndexOf('/');
        if (slashIndex >= 0) {
            baseName = baseName.substring(slashIndex + 1);
        }

        String safe = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? "upload" : safe;
    }
}
