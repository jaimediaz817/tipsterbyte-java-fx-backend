// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-13 (login): email y contraseña en claro.
// [POR QUÉ]: Traduce la request HTTP al comando AutenticarUsuarioComando. El email
//            se valida como formato; la verificación de credenciales ocurre en el
//            caso de uso (nunca en el controller).
// [RELACIONES]: CU-13 → AuthController → AutenticarUsuarioComando (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "email es obligatorio")
        @Email(message = "email con formato inválido")
        String email,

        @NotBlank(message = "password es obligatoria")
        String password) {
}
