package com.melodyHub.service.storage.imagekit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.melodyHub.service.storage.ImageStorageException;
import com.melodyHub.service.storage.ImageUploadResult;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ImageKitStorageServiceTest {
    @Test
    void uploadsCoverInsideTheArtistsCoverFolder() throws ImageStorageException {
        byte[] imageContent = new byte[]{1, 2, 3};
        AtomicReference<byte[]> uploadedContent = new AtomicReference<>();
        AtomicReference<String> uploadedFileName = new AtomicReference<>();
        AtomicReference<String> uploadedFolder = new AtomicReference<>();
        ImageKitUploader uploader = (content, fileName, folder) -> {
            uploadedContent.set(content);
            uploadedFileName.set(fileName);
            uploadedFolder.set(folder);
            return new ImageUploadResult(
                    "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/covers/cover.jpg",
                    "imagekit-file-id",
                    "/artists/son-tung-mtp/covers/cover.jpg"
            );
        };
        ImageKitStorageService service = new ImageKitStorageService(uploader);

        ImageUploadResult result = service.uploadCover("son-tung-mtp", imageContent, "cover.jpg");

        assertArrayEquals(imageContent, uploadedContent.get());
        assertEquals("cover.jpg", uploadedFileName.get());
        assertEquals("/artists/son-tung-mtp/covers/", uploadedFolder.get());
        assertEquals("https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/covers/cover.jpg", result.imageUrl());
        assertEquals("imagekit-file-id", result.fileId());
        assertEquals("/artists/son-tung-mtp/covers/cover.jpg", result.filePath());
    }

    @Test
    void rejectsAnInvalidArtistSlugBeforeUploading() {
        ImageKitUploader uploader = (content, fileName, folder) -> {
            throw new AssertionError("Provider must not receive an invalid folder");
        };
        ImageKitStorageService service = new ImageKitStorageService(uploader);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadCover("../another-artist", new byte[]{1}, "cover.jpg")
        );
    }
}
