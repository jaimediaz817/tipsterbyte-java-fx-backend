// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del caso de uso CU-10 (sincronizar catálogo de países/ligas).
// [POR QUÉ]: Verifica la orquestación: persistir países nuevos por ISO (sin duplicar),
//            obtener ligas por país desde la fuente #5, mapear `anio` a Temporada y
//            persistir ligas en BORRADOR sin duplicar por urlSoccerway.
// [RELACIONES]: HU-10 → CU-10 → ProveedorPaises + ProveedorLigasPorPais +
//               PaisRepository + LigaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarCatalogoUseCaseTest {

    @Mock
    private ProveedorPaises proveedorPaises;
    @Mock
    private ProveedorLigasPorPais proveedorLigasPorPais;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private LigaRepository ligaRepository;

    private SincronizarCatalogoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new SincronizarCatalogoUseCase(
                proveedorPaises, proveedorLigasPorPais, paisRepository, ligaRepository);
    }

    @Test
    void debe_persistir_paises_y_ligas_del_catalogo() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true),
                new PaisFuente("Francia", "/francia/", "80", "FR", "Europa", true)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("LaLiga EA Sports", "League", "", null,
                        "https://co.soccerway.com/espana/laliga-ea-sports/", "2026/2027")));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Francia", 0)).thenReturn(List.of());
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        List<DomainEvent> eventos = casoDeUso.ejecutar();

        verify(paisRepository, times(2)).guardar(any(Pais.class));
        ArgumentCaptor<Liga> captor = ArgumentCaptor.forClass(Liga.class);
        verify(ligaRepository, times(1)).guardar(captor.capture());
        Liga ligaGuardada = captor.getValue();
        assertEquals("LaLiga EA Sports", ligaGuardada.nombre());
        assertEquals("España", ligaGuardada.pais());
        assertEquals(new Temporada(2026, 2027), ligaGuardada.temporada());
        assertEquals("https://co.soccerway.com/espana/laliga-ea-sports/", ligaGuardada.urlSoccerway());
        assertEquals(EstadoLiga.BORRADOR, ligaGuardada.estado());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void debe_no_duplicar_paises_ni_ligas_existentes() {
        Pais existente = new Pais("España", "ES", "Europa", "81", "/espana/", true);
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true)));
        when(paisRepository.buscarPorIsoAlpha2("ES")).thenReturn(Optional.of(existente));
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("LaLiga EA Sports", "League", "", null,
                        "https://co.soccerway.com/espana/laliga-ea-sports/", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway("https://co.soccerway.com/espana/laliga-ea-sports/"))
                .thenReturn(Optional.of(new Liga("LaLiga EA Sports", "España",
                        new Temporada(2026, 2027), "https://co.soccerway.com/espana/laliga-ea-sports/", null)));

        casoDeUso.ejecutar();

        verify(paisRepository, never()).guardar(any());
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_omitir_liga_con_temporada_invalida_y_continuar_con_el_catalogo() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true),
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("Liga Rara", "League", "", null, "https://url-rara", "Grupo 1")));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Válida", "League", "", null, "https://url-valida", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        ArgumentCaptor<Liga> captor = ArgumentCaptor.forClass(Liga.class);
        verify(ligaRepository, times(1)).guardar(captor.capture());
        assertEquals("Liga Válida", captor.getValue().nombre());
        verify(paisRepository, times(2)).guardar(any(Pais.class));
    }
}