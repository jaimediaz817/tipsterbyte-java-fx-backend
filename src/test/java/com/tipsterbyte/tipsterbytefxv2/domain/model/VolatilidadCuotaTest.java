// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios del VO VolatilidadCuota (HU-15).
// [POR QUÉ]: Valida la lógica de cálculo de volatilidad con distintos escenarios:
//            estable (<3%), moderada (3-10%), volátil (≥10%), sin baseline (<2 captures).
// [RELACIONES]: VolatilidadCuota (domain.model), CU-21 (ConsultarCuotasProximasUseCase).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VolatilidadCuotaTest {

    @Test
    void debe_retornar_estable_cuando_variacion_es_menor_a_3_por_ciento() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("2.00"), new BigDecimal("2.05"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.ESTABLE, resultado.clase());
        assertEquals(new BigDecimal("2.50"), resultado.variacionPorcentual());
    }

    @Test
    void debe_retornar_moderada_cuando_variacion_esta_entre_3_y_10_por_ciento() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("2.00"), new BigDecimal("2.10"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.MODERADA, resultado.clase());
        assertEquals(new BigDecimal("5.00"), resultado.variacionPorcentual());
    }

    @Test
    void debe_retornar_volatil_cuando_variacion_supera_10_por_ciento() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("2.00"), new BigDecimal("2.50"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.VOLATIL, resultado.clase());
        assertEquals(new BigDecimal("25.00"), resultado.variacionPorcentual());
    }

    @Test
    void debe_retornar_sin_baseline_cuando_baseline_es_null() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(null, new BigDecimal("2.00"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.SIN_BASELINE, resultado.clase());
        assertNull(resultado.variacionPorcentual());
    }

    @Test
    void debe_retornar_sin_baseline_cuando_ultima_es_null() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(new BigDecimal("2.00"), null);
        assertEquals(VolatilidadCuota.ClaseVolatilidad.SIN_BASELINE, resultado.clase());
    }

    @Test
    void debe_retornar_sin_baseline_cuando_baseline_es_cero() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                BigDecimal.ZERO, new BigDecimal("2.00"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.SIN_BASELINE, resultado.clase());
    }

    @Test
    void debe_retornar_estable_cuando_ambas_cuotas_son_iguales() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("3.50"), new BigDecimal("3.50"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.ESTABLE, resultado.clase());
        assertEquals(new BigDecimal("0.00"), resultado.variacionPorcentual());
    }

    @Test
    void debe_manejar_cuota_exactamente_en_umbral_estable_moderada() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("2.00"), new BigDecimal("2.06"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.MODERADA, resultado.clase());
    }

    @Test
    void debe_manejar_cuota_exactamente_en_umbral_moderada_volatil() {
        VolatilidadCuota resultado = VolatilidadCuota.calcular(
                new BigDecimal("2.00"), new BigDecimal("2.20"));
        assertEquals(VolatilidadCuota.ClaseVolatilidad.VOLATIL, resultado.clase());
    }
}
