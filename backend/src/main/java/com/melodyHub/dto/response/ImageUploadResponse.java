package com.melodyHub.dto.response;

import com.melodyHub.service.storage.ImageUploadResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    private String imageUrl;
    private String fileId;
    private String filePath;

    public static ImageUploadResponse fromResult(ImageUploadResult result) {
        if (result == null) {
            return null;
        }

        return new ImageUploadResponse(result.imageUrl(), result.fileId(), result.filePath());
    }
}
