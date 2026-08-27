// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios del aggregate Estrategia (HU-16).
// [POR QUÉ]: Valida construcción, validaciones y lifecycle de la estrategia.
// [RELACIONES]: Estrategia (domain.model), CU-23.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstrategiaTest {

    private final UUID tipsterId = UUID.randomUUID();

    @Test
    void debe_crear_estrategia_con_criterios() {
        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "1.40",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        Estrategia estrategia = new Estrategia("Mi Estrategia", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(criterio), List.of());

        assertEquals("Mi Estrategia", estrategia.nombre());
        assertTrue(estrategia.activa());
        assertEquals(1, estrategia.criterios().size());
    }

    @Test
    void debe_rechazar_nombre_nulo() {
        assertThrows(DomainException.class, () ->
                new Estrategia(null, tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(), List.of()));
    }

    @Test
    void debe_rechazar_nombre_vacio() {
        assertThrows(DomainException.class, () ->
                new Estrategia("", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(), List.of()));
    }

    @Test
    void debe_rechazar_tipster_id_nulo() {
        assertThrows(DomainException.class, () ->
                new Estrategia("Test", null, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(), List.of()));
    }

    @Test
    void debe_rechazar_max_partidos_invalido() {
        assertThrows(DomainException.class, () ->
                new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                        0, new BigDecimal("0.60"), List.of(), List.of()));
    }

    @Test
    void debe_rechazar_confianza_minima_fuera_de_rango() {
        assertThrows(DomainException.class, () ->
                new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("1.50"), List.of(), List.of()));
    }

    @Test
    void debe_activar_y_desactivar() {
        Estrategia estrategia = new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(), List.of());

        assertTrue(estrategia.activa());
        estrategia.desactivar();
        assertFalse(estrategia.activa());
        estrategia.activar();
        assertTrue(estrategia.activa());
    }

    @Test
    void debe_actualizar_criterios() {
        Estrategia estrategia = new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(), List.of());

        Criterio nuevo = new Criterio(
                Criterio.FuenteCriterio.POSICIONES, "diferencia_posiciones",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "3",
                Criterio.ReferenciaCriterio.AMBOS,
                new BigDecimal("0.30"), 1);

        estrategia.actualizarCriterios(List.of(nuevo));
        assertEquals(1, estrategia.criterios().size());
    }

    @Test
    void debe_rechazar_criterios_vacios() {
        Estrategia estrategia = new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(), List.of());

        assertThrows(DomainException.class, () -> estrategia.actualizarCriterios(List.of()));
    }
}
