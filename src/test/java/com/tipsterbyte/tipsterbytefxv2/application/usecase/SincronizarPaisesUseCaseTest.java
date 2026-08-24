// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-17 (poblar países, granular HU-12 paso 1).
// [POR QUÉ]: Verifica que el poblamiento granular de países invalida cache,
//            persiste nuevos por isoAlpha2 sin duplicar y retorna conteos.
// [RELACIONES]: HU-12 → CU-17 → ProveedorPaises + PaisRepository + CacheLecturas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarPaisesUseCaseTest {

    @Mock
    private ProveedorPaises proveedorPaises;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private CacheLecturas cacheLecturas;

    private SincronizarPaisesUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new SincronizarPaisesUseCase(proveedorPaises, paisRepository, cacheLecturas);
    }

    @Test
    void debe_persistir_paises_nuevos_e_invalidar_cache() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "CO", "CO", "Sudamérica", true),
                new PaisFuente("España", "/espana/", "ES", "ES", "Europa", true)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(paisRepository.buscarTodos()).thenReturn(List.of(
                new Pais("Colombia", "CO", "Sudamérica", "CO", "/colombia/", true),
                new Pais("España", "ES", "Europa", "ES", "/espana/", true)));

        var resultado = casoDeUso.ejecutar();

        verify(cacheLecturas).eliminar("paises");
        verify(paisRepository, org.mockito.Mockito.times(2)).guardar(any(Pais.class));
        assertEquals(2, resultado.nuevos());
        assertEquals(2, resultado.totalPaises());
    }

    @Test
    void debe_no_duplicar_paises_existentes() {
        Pais existente = new Pais("Colombia", "CO", "Sudamérica", "CO", "/colombia/", true);
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "CO", "CO", "Sudamérica", true)));
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(existente));
        when(paisRepository.buscarTodos()).thenReturn(List.of(existente));

        var resultado = casoDeUso.ejecutar();

        verify(paisRepository, never()).guardar(any());
        assertEquals(0, resultado.nuevos());
        assertEquals(1, resultado.totalPaises());
    }
}
