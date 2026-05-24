package com.project.crypto.dto;

public record AuthResponse(String tokenType, String accessToken, Long userId, String username) {
    public static AuthResponse of(String accessToken, Long userId, String username) {
        return new AuthResponse("Bearer", accessToken, userId, username);
    }
}
