// ─────────────────────────────────────────────
// [QUÉ]: Comando de CU-12 (registro): datos para crear un nuevo usuario autenticable.
// [POR QUÉ]: Aísla el contrato de registro (nombre, email, password, rol) del entity
//            Usuario; el controller mapea este comando al dominio.
// [RELACIONES]: CU-12 → RegistrarUsuarioUseCase (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;

public record RegistrarUsuarioComando(
        String nombre,
        String email,
        String password,
        Rol rol) {
}
