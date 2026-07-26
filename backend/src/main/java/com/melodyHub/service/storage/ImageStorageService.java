package com.melodyHub.service.storage;

public interface ImageStorageService {
    ImageUploadResult uploadCover(
            String artistSlug,
            byte[] imageContent,
            String fileName
    ) throws ImageStorageException;

    /**
     * Uploads an image scoped to a user (e.g. an artist avatar chosen before the
     * artist profile exists). Stored under {@code /users/{userId}/uploads/}.
     */
    ImageUploadResult uploadUserImage(
            int userId,
            byte[] imageContent,
            String fileName
    ) throws ImageStorageException;
}
