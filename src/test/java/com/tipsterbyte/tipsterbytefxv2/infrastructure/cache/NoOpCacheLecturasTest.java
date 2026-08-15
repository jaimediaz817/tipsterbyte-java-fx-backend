// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de NoOpCacheLecturas: no guarda, no devuelve nada y eliminar
//        no tiene efecto.
// [POR QUÉ]: Con app.cache.enabled=false el cache es un no-op: los decoradores y los
//            casos de uso de sync deben seguir funcionando sin error (FASE 12).
// [RELACIONES]: Cubre infrastructure.cache.NoOpCacheLecturas (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.cache;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOpCacheLecturasTest {

    private final CacheLecturas noOp = new NoOpCacheLecturas();

    @Test
    void obtener_siempre_devuelve_vacio() {
        noOp.guardar("clave", "valor", Duration.ofMinutes(5));

        assertTrue(noOp.obtener("clave").isEmpty());
    }

    @Test
    void guardar_y_eliminar_no_producen_error() {
        noOp.guardar("clave", "valor", Duration.ofMinutes(5));

        noOp.eliminar("clave");

        assertTrue(noOp.obtener("clave").isEmpty());
    }

    @Test
    void obtener_de_clave_inexistente_devuelve_optional_vacio() {
        assertTrue(noOp.obtener("cualquiera").isEmpty());
    }
}
