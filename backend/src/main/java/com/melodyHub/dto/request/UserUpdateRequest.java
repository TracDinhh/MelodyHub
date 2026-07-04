package com.melodyHub.dto.request;

import com.melodyHub.entity.UserRole;
import com.melodyHub.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
}
