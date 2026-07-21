package com.melodyHub.service.storage;

public record ImageUploadResult(String imageUrl, String fileId, String filePath) {
    public ImageUploadResult {
        imageUrl = requireValue(imageUrl, "imageUrl");
        fileId = requireValue(fileId, "fileId");
        filePath = requireValue(filePath, "filePath");
    }

    private static String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value;
    }
}
