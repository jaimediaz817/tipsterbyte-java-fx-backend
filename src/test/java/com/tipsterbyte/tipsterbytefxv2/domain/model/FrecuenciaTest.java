// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de Frecuencia (domain.model): codificación de la frecuencia
//        amigable (cada N segundos/minutos/horas/días) a cron de 6 segmentos y
//        validación de rangos por unidad.
// [POR QUÉ]: Garantiza que el editor amigable del frontend produce crons válidos y
//            coherentes con el scheduler (CronExpression de Spring).
// [RELACIONES]: Frecuencia → UnidadFrecuencia → GestionarTareasProgramasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrecuenciaTest {

    @Test
    void debe_codificar_segundos_a_cron() {
        assertEquals("0/30 * * * * *", new Frecuencia(30, UnidadFrecuencia.SEGUNDOS).toCronExpression());
    }

    @Test
    void debe_codificar_minutos_a_cron() {
        assertEquals("0 0/15 * * * *", new Frecuencia(15, UnidadFrecuencia.MINUTOS).toCronExpression());
    }

    @Test
    void debe_codificar_horas_a_cron() {
        assertEquals("0 0 */6 * * *", new Frecuencia(6, UnidadFrecuencia.HORAS).toCronExpression());
    }

    @Test
    void debe_codificar_dias_a_cron() {
        assertEquals("0 0 0 */8 * *", new Frecuencia(8, UnidadFrecuencia.DIAS).toCronExpression());
    }

    @Test
    void debe_parsear_unidad_ignorando_mayusculas_y_espacios() {
        Frecuencia f = Frecuencia.of(2, " horas ");
        assertEquals(UnidadFrecuencia.HORAS, f.unidad());
        assertEquals("0 0 */2 * * *", f.toCronExpression());
    }

    @Test
    void debe_rechazar_valor_cero() {
        assertThrows(DomainException.class, () -> new Frecuencia(0, UnidadFrecuencia.MINUTOS));
    }

    @Test
    void debe_rechazar_segundos_mayores_a_59() {
        assertThrows(DomainException.class, () -> new Frecuencia(60, UnidadFrecuencia.SEGUNDOS));
    }

    @Test
    void debe_rechazar_horas_mayores_a_23() {
        assertThrows(DomainException.class, () -> new Frecuencia(24, UnidadFrecuencia.HORAS));
    }

    @Test
    void debe_rechazar_dias_mayores_a_30() {
        assertThrows(DomainException.class, () -> new Frecuencia(31, UnidadFrecuencia.DIAS));
    }

    @Test
    void debe_rechazar_unidad_invalida() {
        assertThrows(DomainException.class, () -> Frecuencia.of(1, "DECADAS"));
    }

    @Test
    void debe_rechazar_unidad_ausente() {
        assertThrows(DomainException.class, () -> Frecuencia.of(1, ""));
    }
}