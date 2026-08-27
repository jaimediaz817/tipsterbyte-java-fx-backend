// ─────────────────────────────────────────────
// [QUÉ]: DTO de request para crear/actualizar una estrategia (HU-16 AC12).
// [POR QUÉ]: El frontend envía la estrategia con sus criterios embebidos.
// [RELACIONES]: EstrategiaController → GestionarEstrategiasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EstrategiaRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull String mercado,
        Integer maxPartidos,
        BigDecimal confianzaMinima,
        @NotEmpty @Valid List<CriterioRequest> criterios,
        List<UUID> ligaIds) {

    public record CriterioRequest(
            @NotBlank String fuente,
            @NotBlank String campo,
            @NotBlank String operador,
            String valor,
            @NotBlank String referencia,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal peso,
            Integer orden) {
    }
}
