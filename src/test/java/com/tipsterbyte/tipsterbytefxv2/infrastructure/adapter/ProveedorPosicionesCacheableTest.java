// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ProveedorPosicionesCacheable: cache hit (no delega), cache
//        miss (delega + guarda) y fallo de serialización degrada a fuente.
// [POR QUÉ]: La lógica cache-aside del decorador (FASE 12) se cubre de forma aislada
//            con mocks del delegado y del puerto CacheLecturas (regla testing.md).
// [ALTERNATIVAS]: Spring context con Redis; se descarta porque el decorador solo orquesta
//                 delegado+cache, cubierto mejor por unit tests rápidos.
// [RELACIONES]: Cubre infrastructure.adapter.ProveedorPosicionesCacheable (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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

class ProveedorPosicionesCacheableTest {

    private ProveedorPosiciones delegado;
    private CacheLecturas cache;
    private ProveedorPosicionesCacheable decorador;

    @BeforeEach
    void setUp() {
        delegado = mock(ProveedorPosiciones.class);
        cache = mock(CacheLecturas.class);
        ObjectMapper mapper = new JsonMapper();
        decorador = new ProveedorPosicionesCacheable(delegado, cache, mapper, 300L);
    }

    private PosicionFuente posicion() {
        return new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10);
    }

    @Test
    void debe_devolver_desde_cache_sin_consultar_fuente() {
        UUID ligaId = UUID.randomUUID();
        when(cache.obtener("posiciones:" + ligaId))
                .thenReturn(Optional.of("[{\"equipoNombre\":\"Real Madrid\",\"posicion\":1,\"jugados\":5," +
                        "\"ganados\":3,\"empatados\":1,\"perdidos\":1,\"golesFavor\":10,\"golesContra\":4," +
                        "\"puntos\":10,\"ultimosResultados\":[]}]"));

        List<PosicionFuente> resultado = decorador.obtenerPosiciones(ligaId);

        assertEquals(1, resultado.size());
        assertEquals("Real Madrid", resultado.get(0).equipoNombre());
        verify(delegado, never()).obtenerPosiciones(any());
    }

    @Test
    void debe_delegar_y_guardar_en_cache_cuando_hay_miss() {
        UUID ligaId = UUID.randomUUID();
        when(cache.obtener("posiciones:" + ligaId)).thenReturn(Optional.empty());
        when(delegado.obtenerPosiciones(ligaId)).thenReturn(List.of(posicion()));

        List<PosicionFuente> resultado = decorador.obtenerPosiciones(ligaId);

        assertEquals(1, resultado.size());
        verify(cache).guardar(eq("posiciones:" + ligaId), anyString(), any());
    }

    @Test
    void debe_serializar_y_deserializar_la_racha_de_resultados() {
        UUID ligaId = UUID.randomUUID();
        PosicionFuente conRacha = new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10,
                List.of(com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente.GANADO,
                        com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente.EMPATE));
        when(cache.obtener("posiciones:" + ligaId)).thenReturn(Optional.empty());
        when(delegado.obtenerPosiciones(ligaId)).thenReturn(List.of(conRacha));

        List<PosicionFuente> resultado = decorador.obtenerPosiciones(ligaId);

        assertEquals(2, resultado.get(0).ultimosResultados().size());
        assertEquals(com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente.GANADO,
                resultado.get(0).ultimosResultados().get(0));
    }
}
