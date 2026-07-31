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
public class ListenHistory {
    private Long id;
    private Integer userId;
    private Integer songId;
    private Integer playedSec;
    private LocalDateTime listenedAt;
}
