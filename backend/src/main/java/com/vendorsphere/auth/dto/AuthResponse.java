package com.vendorsphere.auth.dto;

import com.vendorsphere.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user
) {
}
