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
public class PasswordResetToken {
    private Integer id;
    private Integer userId;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
