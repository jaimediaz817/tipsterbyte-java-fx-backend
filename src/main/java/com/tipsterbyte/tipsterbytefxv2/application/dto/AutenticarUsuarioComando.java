// ─────────────────────────────────────────────
// [QUÉ]: Comando de CU-13 (login): credenciales que se verifican contra el Usuario.
// [POR QUÉ]: Aísla el contrato de login (email + password en claro) del dominio.
// [RELACIONES]: CU-13 → AutenticarUsuarioUseCase (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record AutenticarUsuarioComando(
        String email,
        String password) {
}
