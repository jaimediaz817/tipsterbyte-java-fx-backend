// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-04 (activar liga): la disponibilidad operativa de las
//        fuentes de datos (posiciones, calendario, cuotas) reportada por la request.
// [POR QUÉ]: Traduce la request HTTP en el comando DisponibilidadFuentes que CU-04
//            necesita (BR-001 exige fuentes operativas). La validación estructural
//            (Bean Validation) ocurre en esta capa; la regla de negocio en el dominio.
// [ALTERNATIVAS]: Recibir el enum EstadoLiga en el body; se descarta porque la
//                 activación expresa disponibilidad de fuentes, no un estado final.
// [RELACIONES]: CU-04 → DisponibilidadFuentes (application.dto) → Liga.activar().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActivarLigaRequest(

        @NotNull(message = "posiciones es obligatorio")
        Boolean posiciones,

        @NotNull(message = "calendario es obligatorio")
        Boolean calendario,

        @NotNull(message = "cuotas es obligatorio")
        Boolean cuotas) {
}