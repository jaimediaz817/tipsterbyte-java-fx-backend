// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-14 (país de interés): iso_alpha2 y nombre del país.
// [POR QUÉ]: Traduce la request HTTP en el comando RegistrarPaisInteresComando que
//            CU-14 necesita. La validación estructural (Bean Validation) ocurre en
//            esta capa; la prioridad la deriva el caso de uso.
// [ALTERNATIVAS]: Incluir prioridad en el request; se descarta porque la UI no calcula
//                 orden (POST = al final, PUT = posición en la lista).
// [RELACIONES]: CU-14 → RegistrarPaisInteresComando (application.dto) → PaisInteres.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaisInteresRequest(

        @NotBlank(message = "isoAlpha2 es obligatorio")
        String isoAlpha2,

        @NotBlank(message = "nombre es obligatorio")
        String nombre) {
}