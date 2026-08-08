package com.melodyHub.dto.request;

public record ResetPasswordRequest(String token, String newPassword) {}
