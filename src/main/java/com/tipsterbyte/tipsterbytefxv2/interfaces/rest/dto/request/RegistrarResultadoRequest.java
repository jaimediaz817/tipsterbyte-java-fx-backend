// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-05 (registrar resultado): el marcador final del partido
//        (goles local y visitante) que el administrador o el sistema reportan.
// [POR QUÉ]: Traduce la request HTTP al VO Resultado del dominio. La validación de
//            no-negatividad ya vive en el VO (BR-003); aquí solo se valida la presencia.
// [ALTERNATIVAS]: Enviar el Resultado directamente; se descarta porque acoplaría la
//                 request a un record del dominio sin validación estructural propia.
// [RELACIONES]: CU-05 → Resultado (domain.model) → Partido.asignarResultado().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegistrarResultadoRequest(

        @NotNull(message = "golesLocal es obligatorio")
        Integer golesLocal,

        @NotNull(message = "golesVisitante es obligatorio")
        Integer golesVisitante) {
}