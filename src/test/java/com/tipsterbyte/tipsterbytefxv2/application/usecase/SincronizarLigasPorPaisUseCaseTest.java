// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-18 (poblar ligas por país, granular HU-12 paso 2).
// [POR QUÉ]: Verifica validación de iso, exigencia de país existente, respeto del
//            límite maxLigasPorPais (CU-14), creación de Liga+Temporada, poblamiento
//            de equipos #6 solo si país de interés y tolerancia a fallos.
// [RELACIONES]: HU-12 → CU-18 → ProveedorLigasPorPais + PaisRepository +
//               LigaRepository + PaisInteresRepository + SincronizarEquiposLigaUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarLigasPorPaisUseCaseTest {

    @Mock private ProveedorLigasPorPais proveedorLigasPorPais;
    @Mock private ProveedorEquiposPorLiga proveedorEquiposPorLiga;
    @Mock private PaisRepository paisRepository;
    @Mock private LigaRepository ligaRepository;
    @Mock private PaisInteresRepository paisInteresRepository;
    @Mock private CacheLecturas cacheLecturas;

    private SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    private SincronizarLigasPorPaisUseCase casoDeUso;

    private final Pais colombia = new Pais("Colombia", "CO", "Sudamérica", "CO", "/colombia/", true);

    @BeforeEach
    void setUp() {
        sincronizarEquiposLigaUseCase = new SincronizarEquiposLigaUseCase(
                proveedorEquiposPorLiga, cacheLecturas, ligaRepository);
        casoDeUso = new SincronizarLigasPorPaisUseCase(
                proveedorLigasPorPais, sincronizarEquiposLigaUseCase,
                paisRepository, ligaRepository, paisInteresRepository);
    }

    @Test
    void debe_poblar_ligas_de_un_pais_con_temporada() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(colombia));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Betplay", "League", "", null,
                        "https://co.soccerway.com/colombia/liga-betplay/", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());
        when(ligaRepository.buscarPorPais("Colombia")).thenReturn(List.of());

        var res = casoDeUso.ejecutar("co");

        verify(ligaRepository).guardar(any());
        assertEquals("CO", res.isoAlpha2());
        assertEquals(1, res.ligasCreadas());
    }

    @Test
    void debe_lanzar_422_si_iso_invalido() {
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar("C"));
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(null));
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar("COL"));
    }

    @Test
    void debe_lanzar_422_si_pais_no_existe() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar("CO"));
    }

    @Test
    void debe_respetar_maxLigasPorPais() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(colombia));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(
                Optional.of(new PaisInteres("CO", "Colombia", 1, 1)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 1)).thenReturn(List.of(
                new LigaFuente("Liga A", "League", "", null, "https://url-a", "2026/2027"),
                new LigaFuente("Liga B", "League", "", null, "https://url-b", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());
        when(ligaRepository.buscarPorPais(anyString())).thenReturn(List.of());

        casoDeUso.ejecutar("CO");

        // Solo 1 liga creada pese a que la fuente devolvió 2 (corte local)
        ArgumentCaptor<com.tipsterbyte.tipsterbytefxv2.domain.model.Liga> cap =
                ArgumentCaptor.forClass(com.tipsterbyte.tipsterbytefxv2.domain.model.Liga.class);
        verify(ligaRepository).guardar(cap.capture());
        assertEquals("Liga A", cap.getValue().nombre());
    }

    @Test
    void debe_poblar_equipos_solo_si_pais_de_interes() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(colombia));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(
                Optional.of(new PaisInteres("CO", "Colombia", 1, null)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Betplay", "League", "", null,
                        "https://co.soccerway.com/colombia/liga-betplay/", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());
        when(proveedorEquiposPorLiga.obtenerEquipos(eq("Colombia"), anyString())).thenReturn(List.of(
                new EquipoFuente("Millonarios", "https://escudos/millo.png")));
        when(ligaRepository.buscarPorPais(anyString())).thenReturn(List.of());

        casoDeUso.ejecutar("CO");

        verify(proveedorEquiposPorLiga).obtenerEquipos("Colombia", "Liga Betplay");
    }

    @Test
    void debe_no_llamar_equipos_si_no_es_de_interes() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(colombia));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Betplay", "League", "", null,
                        "https://co.soccerway.com/colombia/liga-betplay/", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());
        when(ligaRepository.buscarPorPais(anyString())).thenReturn(List.of());

        casoDeUso.ejecutar("CO");

        verify(proveedorEquiposPorLiga, never()).obtenerEquipos(anyString(), anyString());
    }

    @Test
    void debe_omitir_liga_con_anio_invalido_y_continuar() {
        when(paisRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(colombia));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Rara", "League", "", null, "https://url-rara", "Grupo 1"),
                new LigaFuente("Liga Ok", "League", "", null, "https://url-ok", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());
        when(ligaRepository.buscarPorPais(anyString())).thenReturn(List.of());

        var res = casoDeUso.ejecutar("CO");

        assertEquals(1, res.ligasCreadas());
    }
}
