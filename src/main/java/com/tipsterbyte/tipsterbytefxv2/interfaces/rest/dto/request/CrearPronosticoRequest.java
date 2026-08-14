// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-06 (crear pronóstico): el tipster, el partido, el mercado,
//        la selección esperada y la cuota de referencia con que se crea el borrador.
// [POR QUÉ]: Traduce la request HTTP al comando CrearPronosticoComando que CU-06
//            consume. La validación estructural ocurre aquí (campos obligatorios,
//            cuota > 0); la regla de cuota > 1.0 (BR-007) la valida el VO Cuota.
// [ALTERNATIVAS]: Exponer los ids como path params; se descarta porque la creación
//                 de un recurso anidado lleva sus referencias en el body.
// [RELACIONES]: CU-06 → CrearPronosticoComando (application.dto) → Pronostico.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearPronosticoRequest(

        @NotNull(message = "tipsterId es obligatorio")
        UUID tipsterId,

        @NotNull(message = "partidoId es obligatorio")
        UUID partidoId,

        @NotNull(message = "mercado es obligatorio")
        Mercado mercado,

        @NotBlank(message = "resultadoEsperado es obligatorio")
        String resultadoEsperado,

        @NotNull(message = "cuotaValor es obligatorio")
        @DecimalMin(value = "0.0", message = "cuotaValor debe ser mayor o igual a 0")
        BigDecimal cuotaValor) {
}