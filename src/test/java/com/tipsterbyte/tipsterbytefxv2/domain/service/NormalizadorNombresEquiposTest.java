// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del normalizador de nombres de equipos (regla centralizada del
//        dominio para el matching por nombre).
// [POR QUÉ]: Las fuentes escriben los nombres distinto (tildes, mayúsculas, espacios
//            extra/dobles). Una sola regla de comparación evita duplicados de equipos
//            en las plantillas (CU-10/CU-01/CU-02 y claves de cache).
// [RELACIONES]: NormalizadorNombresEquipos ← CU-10, CU-01, CU-02, CacheClaves.equipos.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizadorNombresEquiposTest {

    @Test
    void debe_quitar_tildes_y_minusculizar() {
        assertEquals("atletico nacional",
                NormalizadorNombresEquipos.normalizar("Atlético Nacional"));
    }

    @Test
    void debe_recortar_espacios_y_colapsar_dobles() {
        assertEquals("boca juniors",
                NormalizadorNombresEquipos.normalizar("  Boca   Juniors "));
    }

    @Test
    void debe_ignorar_nulos() {
        assertEquals("", NormalizadorNombresEquipos.normalizar(null));
    }

    @Test
    void dos_escrituras_distintas_deben_normalizar_igual() {
        assertEquals(
                NormalizadorNombresEquipos.normalizar("Deportivo Riestra"),
                NormalizadorNombresEquipos.normalizar("DEPORTIVO RIESTRA"));
        assertEquals(
                NormalizadorNombresEquipos.normalizar("Unión Santa Fe"),
                NormalizadorNombresEquipos.normalizar("Union Santa Fe"));
    }
}
