// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios del servicio EvaluadorCriterios (HU-16 AC7/AC8).
// [POR QUÉ]: Valida la evaluación de cada tipo de criterio contra datos del partido.
// [RELACIONES]: EvaluadorCriterios → CU-24 EvaluarEstrategiaUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluadorCriteriosTest {

    @Test
    void debe_pass_cuando_cuota_supera_umbral() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "1.40",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, Map.of("cuota_1x", new BigDecimal("2.00")), null, null, false);

        assertTrue(senal.pass());
        assertEquals(new BigDecimal("2.00"), senal.valorObservado());
    }

    @Test
    void debe_fail_cuando_cuota_no_alcanza_umbral() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "2.00",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, Map.of("cuota_1x", new BigDecimal("1.50")), null, null, false);

        assertFalse(senal.pass());
    }

    @Test
    void debe_retornar_son_datos_cuando_no_hay_cuotas() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "1.40",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, Map.of(), null, null, false);

        assertFalse(senal.pass());
        assertEquals("SIN_DATOS", senal.estado());
    }

    @Test
    void debe_evaluar_posiciones() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.POSICIONES, "diferencia_posiciones",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "3",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.30"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, null, Map.of("diferencia_posiciones", 5), null, false);

        assertTrue(senal.pass());
        assertEquals(new BigDecimal("5"), senal.valorObservado());
    }

    @Test
    void debe_evaluar_zona_descenso() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.ZONA_DESCENSO, "en_zona_descenso",
                Criterio.OperadorCriterio.IGUAL, "true",
                Criterio.ReferenciaCriterio.VISITANTE,
                new BigDecimal("0.20"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, null, null, null, true);

        assertTrue(senal.pass());
    }

    @Test
    void debe_fail_zona_descenso_cuando_no_esta_en_zona() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.ZONA_DESCENSO, "en_zona_descenso",
                Criterio.OperadorCriterio.IGUAL, "true",
                Criterio.ReferenciaCriterio.VISITANTE,
                new BigDecimal("0.20"), 1);

        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, null, null, null, false);

        assertFalse(senal.pass());
    }

    @Test
    void debe_evaluar_forma_con_max_letra() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.FORMA, "ultimos_5",
                Criterio.OperadorCriterio.CONTIENE, "max_1_P",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        // 1 derrota (P) en 5 partidos → PASS
        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, null, null,
                Map.of("LOCAL", List.of("G", "E", "G", "P", "G")), false);

        assertTrue(senal.pass());
    }

    @Test
    void debe_fail_forma_cuando_supera_max() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.FORMA, "ultimos_5",
                Criterio.OperadorCriterio.CONTIENE, "max_1_P",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        // 2 derrotas → FAIL
        EvaluadorCriterios.SenalCriterio senal = EvaluadorCriterios.evaluar(
                criterio, null, null,
                Map.of("LOCAL", List.of("G", "P", "P", "G", "E")), false);

        assertFalse(senal.pass());
    }
}
