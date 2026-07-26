package com.melodyHub.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistRequest {
    private Integer id;
    private Integer userId;
    private String artistName;
    private String slug;
    private String bio;
    private String imageUrl;
    private ArtistRequestStatus status;
    private String reviewNote;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
