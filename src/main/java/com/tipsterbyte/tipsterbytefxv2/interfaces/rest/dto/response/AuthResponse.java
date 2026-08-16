// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de autenticación: token JWT y datos del usuario autenticado.
// [POR QUÉ]: Devuelve al cliente el token que debe enviar como "Authorization: Bearer"
//            en cada request posterior, junto con identidad y rol para la UI.
// [RELACIONES]: AuthController → AutenticacionResultado (CU-13).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthResponse(
        UUID usuarioId,
        String nombre,
        String email,
        Rol rol,
        String token,
        LocalDateTime fechaCreacion) {
}
