// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-12 (registro): nombre, email, contraseña y rol del usuario.
// [POR QUÉ]: Traduce la request HTTP al comando RegistrarUsuarioComando. La validación
//            de formato (email, password mínimo) se hace aquí con Bean Validation;
//            el VO Email del dominio valida a nivel de modelo.
// [ALTERNATIVAS]: Validar solo en el dominio; se descarta porque el contrato HTTP debe
//                 fallar temprano con 400 (mensajes claros por campo).
// [RELACIONES]: CU-12 → AuthController → RegistrarUsuarioComando (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioRequest(

        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotBlank(message = "email es obligatorio")
        @Email(message = "email con formato inválido")
        String email,

        @NotBlank(message = "password es obligatoria")
        @Size(min = 6, message = "password debe tener al menos 6 caracteres")
        String password,

        @NotNull(message = "rol es obligatorio")
        Rol rol) {
}
