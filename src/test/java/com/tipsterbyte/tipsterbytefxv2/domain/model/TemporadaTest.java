// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity Temporada (torneo dentro de una liga).
// [POR QUÉ]: Verifica las invariantes: identidad y ligaId obligatorios, años válidos
//            (fin > inicio, positivos), semestre opcional validado (1|2 si presente),
//            nombre opcional normalizado y estado por defecto PLANIFICADA.
// [RELACIONES]: Aggregate de Liga (1:N); referida por Partido y DetalleFuenteExtraccion.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemporadaTest {

    private static final UUID LIGA_ID = UUID.randomUUID();

    @Test
    void debe_aceptar_temporada_con_fin_posterior() {
        assertDoesNotThrow(() -> new Temporada(LIGA_ID, "2025/2026", null, 2025, 2026, null));
    }

    @Test
    void debe_rechazar_temporada_con_fin_igual_al_inicio() {
        assertThrows(DomainException.class,
                () -> new Temporada(LIGA_ID, "2025/2025", null, 2025, 2025, null));
    }

    @Test
    void debe_rechazar_temporada_con_fin_anterior() {
        assertThrows(DomainException.class,
                () -> new Temporada(LIGA_ID, "2026/2025", null, 2026, 2025, null));
    }

    @Test
    void debe_rechazar_anios_no_positivos() {
        assertThrows(DomainException.class,
                () -> new Temporada(LIGA_ID, "0/2026", null, 0, 2026, null));
    }

    @Test
    void debe_aceptar_semestre_valido_y_rechazar_invalido() {
        assertDoesNotThrow(() -> new Temporada(LIGA_ID, "Apertura", 1, 2025, 2026, null));
        assertThrows(DomainException.class,
                () -> new Temporada(LIGA_ID, "Apertura", 3, 2025, 2026, null));
    }

    @Test
    void debe_asignar_estado_planificada_por_defecto() {
        Temporada temporada = new Temporada(LIGA_ID, "2025/2026", null, 2025, 2026, null);
        assertEquals(EstadoTemporada.PLANIFICADA, temporada.estado());
    }

    @Test
    void debe_normalizar_nombre_y_permitir_nulo() {
        assertEquals("Apertura", new Temporada(LIGA_ID, " Apertura ", null, 2025, 2026, null).nombre());
        assertNull(new Temporada(LIGA_ID, null, null, 2025, 2026, null).nombre());
    }

    @Test
    void debe_rechazar_liga_id_nulo() {
        assertThrows(DomainException.class,
                () -> new Temporada(null, "2025/2026", null, 2025, 2026, null));
    }
}
