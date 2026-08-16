// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ProveedorCalendarioCacheable: cache hit (no delega), cache
//        miss (delega + guarda) y serialización de LocalDateTime en el DTO.
// [POR QUÉ]: La lógica cache-aside del decorador (FASE 12) se cubre de forma aislada
//            con mocks del delegado y del puerto CacheLecturas (regla testing.md).
// [ALTERNATIVAS]: Spring context con Redis; se descarta porque el decorador solo orquesta
//                 delegado+cache, cubierto mejor por unit tests rápidos.
// [RELACIONES]: Cubre infrastructure.adapter.ProveedorCalendarioCacheable (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProveedorCalendarioCacheableTest {

    private ProveedorCalendario delegado;
    private CacheLecturas cache;
    private ProveedorCalendarioCacheable decorador;

    @BeforeEach
    void setUp() {
        delegado = mock(ProveedorCalendario.class);
        cache = mock(CacheLecturas.class);
        ObjectMapper mapper = new JsonMapper();
        decorador = new ProveedorCalendarioCacheable(delegado, cache, mapper, 300L);
    }

    @Test
    void debe_devolver_desde_cache_sin_consultar_fuente() {
        UUID ligaId = UUID.randomUUID();
        when(cache.obtener("calendario:" + ligaId))
                .thenReturn(Optional.of("[{\"equipoLocalNombre\":\"Real Madrid\",\"equipoVisitanteNombre\":" +
                        "\"FC Barcelona\",\"fechaHora\":\"2026-03-01T20:00:00\"}]"));

        List<PartidoFuente> resultado = decorador.obtenerCalendario(ligaId);

        assertEquals(1, resultado.size());
        assertEquals("Real Madrid", resultado.get(0).equipoLocalNombre());
        assertEquals(LocalDateTime.of(2026, 3, 1, 20, 0), resultado.get(0).fechaHora());
        verify(delegado, never()).obtenerCalendario(any());
    }

    @Test
    void debe_delegar_y_guardar_en_cache_cuando_hay_miss() {
        UUID ligaId = UUID.randomUUID();
        when(cache.obtener("calendario:" + ligaId)).thenReturn(Optional.empty());
        when(delegado.obtenerCalendario(ligaId)).thenReturn(List.of(
                new PartidoFuente("Real Madrid", "FC Barcelona",
                        LocalDateTime.of(2026, 3, 1, 20, 0), 4)));

        List<PartidoFuente> resultado = decorador.obtenerCalendario(ligaId);

        assertEquals(1, resultado.size());
        verify(cache).guardar(eq("calendario:" + ligaId), anyString(), any());
    }
}
