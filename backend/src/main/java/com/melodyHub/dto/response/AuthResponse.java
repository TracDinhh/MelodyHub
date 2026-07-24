package com.melodyHub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UserResponse user;
    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private String refreshToken;
    private long refreshExpiresInSeconds;
}
