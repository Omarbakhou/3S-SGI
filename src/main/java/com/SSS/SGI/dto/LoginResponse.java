package com.SSS.SGI.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String type,
        Long id,
        String email,
        List<String> roles,
        long expiresIn) {
}
