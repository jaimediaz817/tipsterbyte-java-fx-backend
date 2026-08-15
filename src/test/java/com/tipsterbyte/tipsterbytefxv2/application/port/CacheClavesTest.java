// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CacheClaves: formato canónico de claves del cache-aside.
// [POR QUÉ]: Fija el contrato de claves compartido entre decoradores (guardan/leen) y
//            casos de uso de sincronización (invalidan) para que la invalidación siempre
//            coincida con la clave leída (FASE 12).
// [RELACIONES]: Cubre application.port.CacheClaves (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheClavesTest {

    @Test
    void debe_generar_clave_de_posiciones_con_ligaId() {
        UUID ligaId = UUID.randomUUID();

        assertEquals("posiciones:" + ligaId, CacheClaves.posiciones(ligaId));
    }

    @Test
    void debe_generar_clave_de_calendario_con_ligaId() {
        UUID ligaId = UUID.randomUUID();

        assertEquals("calendario:" + ligaId, CacheClaves.calendario(ligaId));
    }

    @Test
    void debe_generar_clave_de_cuotas_con_partidoId() {
        UUID partidoId = UUID.randomUUID();

        assertEquals("cuotas:" + partidoId, CacheClaves.cuotas(partidoId));
    }
}
