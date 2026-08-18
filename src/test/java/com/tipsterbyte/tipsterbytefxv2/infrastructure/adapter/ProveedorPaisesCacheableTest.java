// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ProveedorPaisesCacheable: cache hit (no delega en el
//        scraper), cache miss (delega + guarda) y serialización/deserialización JSON
//        del DTO PaisFuente.
// [POR QUÉ]: La lógica cache-aside del decorador (FASE 12.6, catálogo de países) se
//            cubre de forma aislada con mocks del delegado y del puerto CacheLecturas
//            (regla testing.md): garantiza que listar países disponibles y validar un
//            favorito (CU-14) no golpeen la fuente externa dentro del TTL.
// [RELACIONES]: Cubre infrastructure.adapter.ProveedorPaisesCacheable (FASE 12.6).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProveedorPaisesCacheableTest {

    private ProveedorPaises delegado;
    private CacheLecturas cache;
    private ProveedorPaisesCacheable decorador;

    @BeforeEach
    void setUp() {
        delegado = mock(ProveedorPaises.class);
        cache = mock(CacheLecturas.class);
        ObjectMapper mapper = new JsonMapper();
        decorador = new ProveedorPaisesCacheable(delegado, cache, mapper, 2592000L);
    }

    private PaisFuente unPais() {
        return new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true);
    }

    @Test
    void debe_devolver_desde_cache_sin_consultar_fuente() {
        when(cache.obtener("paises")).thenReturn(Optional.of(
                "[{\"nombre\":\"Colombia\",\"href\":\"/colombia/\",\"code\":\"81\"," +
                        "\"isoAlpha2\":\"CO\",\"continente\":\"Sudamérica\",\"mapeado\":true}]"));

        List<PaisFuente> resultado = decorador.obtenerPaises();

        assertEquals(1, resultado.size());
        assertEquals("Colombia", resultado.get(0).nombre());
        assertEquals("CO", resultado.get(0).isoAlpha2());
        verify(delegado, never()).obtenerPaises();
    }

    @Test
    void debe_delegar_y_guardar_en_cache_cuando_hay_miss() {
        when(cache.obtener("paises")).thenReturn(Optional.empty());
        when(delegado.obtenerPaises()).thenReturn(List.of(unPais()));

        List<PaisFuente> resultado = decorador.obtenerPaises();

        assertEquals(1, resultado.size());
        verify(delegado).obtenerPaises();
        verify(cache).guardar(eq("paises"), anyString(), any());
    }
}