// ─────────────────────────────────────────────
// [QUÉ]: Resultado de CU-13 (login): usuario autenticado y token JWT emitido.
// [POR QUÉ]: Devuelve lo mínimo que el controller necesita para responder: el token
//            de sesión y datos de identidad/rol para el cliente.
// [RELACIONES]: CU-13 → AutenticarUsuarioUseCase (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;

import java.time.LocalDateTime;
import java.util.UUID;

public record AutenticacionResultado(
        UUID usuarioId,
        String nombre,
        String email,
        Rol rol,
        String token,
        LocalDateTime fechaCreacion) {
}
