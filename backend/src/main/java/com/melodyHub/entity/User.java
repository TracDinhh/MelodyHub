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
public class User {
    private Integer id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private UserRole role = UserRole.USER;
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime premiumUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isPremium() {
        return premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now());
    }
}
