package com.melodyHub.service.storage;

public interface ImageStorageService {
    ImageUploadResult uploadCover(
            String artistSlug,
            byte[] imageContent,
            String fileName
    ) throws ImageStorageException;
}
