package com.melodyHub.service.storage.imagekit;

import com.melodyHub.service.storage.ImageStorageException;
import com.melodyHub.service.storage.ImageUploadResult;

@FunctionalInterface
interface ImageKitUploader {
    ImageUploadResult upload(
            byte[] imageContent,
            String fileName,
            String folder
    ) throws ImageStorageException;
}
