// ─────────────────────────────────────────────
// [QUÉ]: VO que representa el resultado del cálculo de volatilidad de una cuota
//        comparando la última captura contra la BASELINE (primera captura en la ventana).
// [POR QUÉ]: HU-15 AC1/AC4 — el frontend renderiza un badge de color según la clase;
//            el cálculo vive server-side para que los umbrales sean configurables
//            desde properties sin tocar el frontend.
// [ALTERNATIVAS]: Calcular en el frontend; se descarta porque rompe la separación
//                 de responsabilidades y duplica lógica.
// [RELACIONES]: ConsultarCuotasProximasUseCase (calcula) + CuotaProximaResponse (expone).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record VolatilidadCuota(
        ClaseVolatilidad clase,
        BigDecimal variacionPorcentual) {

    public enum ClaseVolatilidad {
        ESTABLE, MODERADA, VOLATIL, SIN_BASELINE
    }

    // [QUÉ]: Calcula la volatilidad comparando baseline vs última captura.
    // [POR QUÉ]: Fórmula: |última - baseline| / baseline * 100.
    //            <3% → ESTABLE, 3-10% → MODERADA, ≥10% → VOLATIL, <2 capturas → SIN_BASELINE.
    public static VolatilidadCuota calcular(BigDecimal baseline, BigDecimal ultima) {
        if (baseline == null || ultima == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            return new VolatilidadCuota(ClaseVolatilidad.SIN_BASELINE, null);
        }
        BigDecimal variacion = ultima.subtract(baseline).abs()
                .divide(baseline, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        ClaseVolatilidad clase;
        if (variacion.compareTo(new BigDecimal("3")) < 0) {
            clase = ClaseVolatilidad.ESTABLE;
        } else if (variacion.compareTo(new BigDecimal("10")) < 0) {
            clase = ClaseVolatilidad.MODERADA;
        } else {
            clase = ClaseVolatilidad.VOLATIL;
        }
        return new VolatilidadCuota(clase, variacion);
    }
}
