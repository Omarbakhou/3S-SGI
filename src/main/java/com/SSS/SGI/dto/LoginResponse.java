package com.SSS.SGI.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String type,
        Long id,
        String email,
        String nom,
        String prenom,
        List<String> roles,
        long expiresIn) {
}
