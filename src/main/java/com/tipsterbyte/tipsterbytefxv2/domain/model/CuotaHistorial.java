// ─────────────────────────────────────────────
// [QUÉ]: Registro de dominio de una observación de cuota (append-only).
//        Cada fila es una captura puntual de una cuota para un partido concreto.
// [POR QUÉ]: HU-14 AC4.5 — el historial permite a HU-15 calcular volatilidad y
//            mostrar series temporales. La escritura es incondicional (sin
//            deduplicación) para no perder rebotes.
// [ALTERNATIVAS]: Guardar solo la última cuota; se descarta porque pierde la serie.
// [RELACIONES]: CuotaHistorialRepository (persistencia) + SincronizarCuotasUseCase
//               (escritura) + HU-15 (lectura).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CuotaHistorial(
        UUID id,
        UUID partidoId,
        Mercado mercado,
        String seleccion,
        BigDecimal valor,
        String fuente,
        Instant capturadaEn) {

    public CuotaHistorial(UUID partidoId, Mercado mercado, String seleccion,
                           BigDecimal valor, String fuente) {
        this(UUID.randomUUID(), partidoId, mercado, seleccion, valor, fuente, Instant.now());
    }
}
