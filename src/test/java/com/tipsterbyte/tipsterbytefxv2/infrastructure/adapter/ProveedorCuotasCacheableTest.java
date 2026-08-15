// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ProveedorCuotasCacheable: cache hit (no delega), cache miss
//        (delega + guarda) y serialización de enum Mercado + BigDecimal.
// [POR QUÉ]: La lógica cache-aside del decorador (FASE 12) se cubre de forma aislada
//            con mocks del delegado y del puerto CacheLecturas (regla testing.md).
// [ALTERNATIVAS]: Spring context con Redis; se descarta porque el decorador solo orquesta
//                 delegado+cache, cubierto mejor por unit tests rápidos.
// [RELACIONES]: Cubre infrastructure.adapter.ProveedorCuotasCacheable (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
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

class ProveedorCuotasCacheableTest {

    private ProveedorCuotas delegado;
    private CacheLecturas cache;
    private ProveedorCuotasCacheable decorador;

    @BeforeEach
    void setUp() {
        delegado = mock(ProveedorCuotas.class);
        cache = mock(CacheLecturas.class);
        ObjectMapper mapper = new JsonMapper();
        decorador = new ProveedorCuotasCacheable(delegado, cache, mapper, 120L);
    }

    @Test
    void debe_devolver_desde_cache_sin_consultar_fuente() {
        UUID partidoId = UUID.randomUUID();
        when(cache.obtener("cuotas:" + partidoId))
                .thenReturn(Optional.of("[{\"mercado\":\"UNO_X_DOS\",\"valor\":1.85}]"));

        List<CuotaFuente> resultado = decorador.obtenerCuotas(partidoId);

        assertEquals(1, resultado.size());
        assertEquals(Mercado.UNO_X_DOS, resultado.get(0).mercado());
        assertEquals(new BigDecimal("1.85"), resultado.get(0).valor());
        verify(delegado, never()).obtenerCuotas(any());
    }

    @Test
    void debe_delegar_y_guardar_en_cache_cuando_hay_miss() {
        UUID partidoId = UUID.randomUUID();
        when(cache.obtener("cuotas:" + partidoId)).thenReturn(Optional.empty());
        when(delegado.obtenerCuotas(partidoId)).thenReturn(List.of(
                new CuotaFuente(Mercado.UNO_X_DOS, new BigDecimal("1.85")),
                new CuotaFuente(Mercado.DOBLE_OPORTUNIDAD, new BigDecimal("1.45"))));

        List<CuotaFuente> resultado = decorador.obtenerCuotas(partidoId);

        assertEquals(2, resultado.size());
        verify(cache).guardar(eq("cuotas:" + partidoId), anyString(), any());
    }
}
