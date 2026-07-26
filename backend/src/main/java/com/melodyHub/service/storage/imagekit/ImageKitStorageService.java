package com.melodyHub.service.storage.imagekit;

import com.melodyHub.config.ImageKitConfig;
import com.melodyHub.service.storage.ImageStorageException;
import com.melodyHub.service.storage.ImageStorageService;
import com.melodyHub.service.storage.ImageUploadResult;
import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ImageKitStorageService implements ImageStorageService {
    private static final Pattern ARTIST_SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final ImageKitUploader uploader;

    public ImageKitStorageService() {
        this(createUploader(ImageKitConfig.fromEnvironment()));
    }

    ImageKitStorageService(ImageKitUploader uploader) {
        this.uploader = Objects.requireNonNull(uploader, "uploader must not be null");
    }

    @Override
    public ImageUploadResult uploadCover(
            String artistSlug,
            byte[] imageContent,
            String fileName
    ) throws ImageStorageException {
        validateArtistSlug(artistSlug);
        validateImageContent(imageContent);
        validateFileName(fileName);

        String folder = "/artists/" + artistSlug + "/covers/";
        return uploader.upload(imageContent, fileName, folder);
    }

    @Override
    public ImageUploadResult uploadUserImage(
            int userId,
            byte[] imageContent,
            String fileName
    ) throws ImageStorageException {
        validateUserId(userId);
        validateImageContent(imageContent);
        validateFileName(fileName);

        String folder = "/users/" + userId + "/uploads/";
        return uploader.upload(imageContent, fileName, folder);
    }

    private static ImageKitUploader createUploader(ImageKitConfig config) {
        ImageKitClient client = ImageKitOkHttpClient.builder()
                .privateKey(config.getPrivateKey())
                .build();

        return (imageContent, fileName, folder) -> uploadWithImageKit(
                client,
                config,
                imageContent,
                fileName,
                folder
        );
    }

    private static ImageUploadResult uploadWithImageKit(
            ImageKitClient client,
            ImageKitConfig config,
            byte[] imageContent,
            String fileName,
            String folder
    ) throws ImageStorageException {
        try {
            FileUploadParams uploadParams = FileUploadParams.builder()
                    .file(imageContent)
                    .fileName(fileName)
                    .folder(folder)
                    .publicKey(config.getPublicKey())
                    .useUniqueFileName(true)
                    .build();
            FileUploadResponse response = client.files().upload(uploadParams);
            String filePath = getRequired(response.filePath(), "filePath");
            String imageUrl = response.url().orElseGet(
                    () -> buildImageUrl(config.getUrlEndpoint(), filePath)
            );

            return new ImageUploadResult(
                    imageUrl,
                    getRequired(response.fileId(), "fileId"),
                    filePath
            );
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ImageStorageException("ImageKit cover upload failed", exception);
        }
    }

    private static String getRequired(Optional<String> value, String fieldName) throws ImageStorageException {
        if (value.isEmpty() || value.get().isBlank()) {
            throw new ImageStorageException("ImageKit response is missing " + fieldName);
        }

        return value.get();
    }

    private static String buildImageUrl(String urlEndpoint, String filePath) {
        String normalizedEndpoint = urlEndpoint.endsWith("/")
                ? urlEndpoint.substring(0, urlEndpoint.length() - 1)
                : urlEndpoint;
        String normalizedPath = filePath.startsWith("/") ? filePath : "/" + filePath;
        return normalizedEndpoint + normalizedPath;
    }

    private static void validateUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be a positive integer");
        }
    }

    private static void validateArtistSlug(String artistSlug) {
        if (artistSlug == null || !ARTIST_SLUG_PATTERN.matcher(artistSlug).matches()) {
            throw new IllegalArgumentException("artistSlug must be a lowercase URL slug");
        }
    }

    private static void validateImageContent(byte[] imageContent) {
        if (imageContent == null || imageContent.length == 0) {
            throw new IllegalArgumentException("imageContent must not be empty");
        }
    }

    private static void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
    }
}
