// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de MDCTaskContext: el contexto MDC (taskName, executionId,
//        ligaId) se enriquece al preparar una tarea y se limpia al llamar clear().
// [POR QUÉ]: Verifica que los logs JSON del scheduler queden correlacionados por
//            executionId sin fugas de contexto entre hilos (limpieza en finally).
// [RELACIONES]: MDCTaskContext → CatalogoScheduler (infrastructure.adapter).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MDCTaskContextTest {

    @AfterEach
    void limpiarMdc() {
        MDCTaskContext.clear();
    }

    @Test
    void debe_enriquecer_mdc_con_datos_de_la_tarea() {
        UUID ligaId = UUID.randomUUID();
        TareaProgramada tarea = new TareaProgramada(UUID.randomUUID(), ligaId,
                TipoFuenteExtraccion.STANDINGS, "1", "* * * * * *", true, "2026-01-01T00:00:00Z", null);

        MDCTaskContext.putTaskContext(tarea, "exec-1");

        assertEquals("STANDINGS", MDC.get("taskName"));
        assertEquals("exec-1", MDC.get("executionId"));
        assertEquals(ligaId.toString(), MDC.get("ligaId"));
    }

    @Test
    void debe_usar_global_cuando_la_tarea_es_de_catalogo() {
        TareaProgramada tarea = new TareaProgramada(UUID.randomUUID(), null, null,
                "0", "* * * * * *", true, "2026-01-01T00:00:00Z", null);

        MDCTaskContext.putTaskContext(tarea, "exec-2");

        assertEquals("GLOBAL", MDC.get("taskName"));
        assertEquals("null", MDC.get("ligaId"));
    }

    @Test
    void debe_limpiar_todas_las_claves_al_llamar_clear() {
        TareaProgramada tarea = new TareaProgramada(UUID.randomUUID(), UUID.randomUUID(),
                TipoFuenteExtraccion.CALENDAR, "1", "* * * * * *", true, "2026-01-01T00:00:00Z", null);
        MDCTaskContext.putTaskContext(tarea, "exec-3");

        MDCTaskContext.clear();

        assertNull(MDC.get("taskName"));
        assertNull(MDC.get("executionId"));
        assertNull(MDC.get("ligaId"));
    }
}