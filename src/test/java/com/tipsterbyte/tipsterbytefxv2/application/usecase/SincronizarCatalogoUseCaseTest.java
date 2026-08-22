// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del caso de uso CU-10 (sincronizar catálogo de países/ligas).
// [POR QUÉ]: Verifica la orquestación: persistir países nuevos por ISO (sin duplicar),
//            obtener ligas por país desde la fuente #5, mapear `anio` a Temporada de
//            catálogo (PLANIFICADA, nombre = anio) y persistir ligas en BORRADOR sin
//            duplicar por urlSoccerway; prioridad y límite maxLigasPorPais desde CU-14.
// [RELACIONES]: HU-10 → CU-10 → ProveedorPaises + ProveedorLigasPorPais +
//               PaisRepository + LigaRepository + PaisInteresRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
    private ProveedorEquiposPorLiga proveedorEquiposPorLiga;
    private SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private PaisInteresRepository paisInteresRepository;
    @Mock
    private CacheLecturas cacheLecturas;

    private SincronizarCatalogoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        // [POR QUÉ]: CU-16 real sobre sus dependencias mockeadas: ejercita el matching
        //            normalizado y la mutación de la plantilla de verdad.
        sincronizarEquiposLigaUseCase = new SincronizarEquiposLigaUseCase(
                proveedorEquiposPorLiga, cacheLecturas, ligaRepository);
        casoDeUso = new SincronizarCatalogoUseCase(
                proveedorPaises, proveedorLigasPorPais, sincronizarEquiposLigaUseCase,
                paisRepository, ligaRepository, paisInteresRepository, cacheLecturas);
    }

    @Test
    void debe_persistir_paises_y_ligas_del_catalogo_con_su_temporada() {
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
        assertEquals(1, ligaGuardada.getTemporadas().size());
        Temporada temporada = ligaGuardada.getTemporadas().iterator().next();
        assertEquals(temporada.ligaId(), ligaGuardada.id());
        assertEquals("2026/2027", temporada.nombre());
        assertEquals(2026, temporada.anioInicio());
        assertEquals(2027, temporada.anioFin());
        assertEquals(EstadoTemporada.PLANIFICADA, temporada.estado());
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
                        "https://co.soccerway.com/espana/laliga-ea-sports/", null)));

        casoDeUso.ejecutar();

        verify(paisRepository, never()).guardar(any());
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_priorizar_paises_de_interes_sin_omitir_el_resto() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true),
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true),
                new PaisFuente("Francia", "/francia/", "81", "FR", "Europa", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, null),
                new PaisInteres("ES", "España", 2, null)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais(anyString(), anyInt())).thenReturn(List.of());

        casoDeUso.ejecutar();

        InOrder inOrder = inOrder(paisRepository);
        inOrder.verify(paisRepository).guardar(argThat(p -> p.nombre().equals("Colombia")));
        inOrder.verify(paisRepository).guardar(argThat(p -> p.nombre().equals("España")));
        inOrder.verify(paisRepository).guardar(argThat(p -> p.nombre().equals("Francia")));
        verify(paisRepository, times(3)).guardar(any(Pais.class));
    }

    @Test
    void debe_aplicar_max_ligas_por_pais_del_pais_de_interes() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, 2)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, 2)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        // El límite viaja también a la fuente #5 (param limit) para no scrapear de más.
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 2)).thenReturn(List.of(
                new LigaFuente("Liga A", "League", "", null, "https://url-a", "2026/2027"),
                new LigaFuente("Liga B", "League", "", null, "https://url-b", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        ArgumentCaptor<Liga> captor = ArgumentCaptor.forClass(Liga.class);
        verify(ligaRepository, times(2)).guardar(captor.capture());
        assertEquals("Liga A", captor.getAllValues().get(0).nombre());
        assertEquals("Liga B", captor.getAllValues().get(1).nombre());
    }

    @Test
    void debe_cortar_localmente_si_la_fuente_ignora_el_limite() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, 2)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, 2)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        // La fuente devuelve más ligas de las pedidas: el corte local garantiza el tope.
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 2)).thenReturn(List.of(
                new LigaFuente("Liga A", "League", "", null, "https://url-a", "2026/2027"),
                new LigaFuente("Liga B", "League", "", null, "https://url-b", "2026/2027"),
                new LigaFuente("Liga C", "League", "", null, "https://url-c", "2026/2027")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        verify(ligaRepository, times(2)).guardar(any(Liga.class));
    }

    @Test
    void debe_poblar_equipos_solo_de_ligas_de_paises_de_interes() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true),
                new PaisFuente("Francia", "/francia/", "81", "FR", "Europa", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("Colombia", 0)).thenReturn(List.of(
                new LigaFuente("Liga Betplay", "League", "", null,
                        "https://co.soccerway.com/colombia/liga-betplay/", "2026/2027")));
        when(proveedorLigasPorPais.obtenerLigasPorPais("Francia", 0)).thenReturn(List.of());
        // La fuente #6 devuelve plantilla con escudo (logo_url).
        when(proveedorEquiposPorLiga.obtenerEquipos("Colombia", "Liga Betplay")).thenReturn(List.of(
                new EquipoFuente("Millonarios", "https://escudos/millonarios.png"),
                new EquipoFuente("Atlético Nacional", "https://escudos/nal.png")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        ArgumentCaptor<Liga> captor = ArgumentCaptor.forClass(Liga.class);
        verify(ligaRepository).guardar(captor.capture());
        Liga liga = captor.getValue();
        assertEquals(2, liga.equipos().size());
        assertEquals("Millonarios", liga.equipos().get(0).nombre());
        assertEquals("https://escudos/millonarios.png", liga.equipos().get(0).logoUrl());
        // El país SIN preferencia no consume la fuente #6.
        verify(proveedorEquiposPorLiga, never()).obtenerEquipos(eq("Francia"), anyString());
    }

    @Test
    void debe_reusar_equipo_por_nombre_normalizado_y_actualizar_escudo() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais(anyString(), anyInt())).thenReturn(List.of(
                new LigaFuente("Liga", "League", "", null, "https://url-liga", "2026/2027")));
        // La fuente trae "Atletico Nacional" (sin tilde): matchea con el existente
        // "Atlético Nacional" y SOLO actualiza su escudo (no crea duplicado).
        // La fuente devuelve DOS entradas que son el mismo equipo con escritura distinta
        // ("Atlético..." y "Atletico..."): la segunda matchea a la primera (normalizada),
        // NO se crea duplicado y solo se actualiza su escudo.
        when(proveedorEquiposPorLiga.obtenerEquipos(anyString(), anyString())).thenReturn(List.of(
                new EquipoFuente("Atlético Nacional", "https://escudos/viejo.png"),
                new EquipoFuente("Atletico Nacional", "https://escudos/nuevo.png")));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        ArgumentCaptor<Liga> captor = ArgumentCaptor.forClass(Liga.class);
        verify(ligaRepository).guardar(captor.capture());
        Liga liga = captor.getValue();
        assertEquals(1, liga.equipos().size(), "no duplica por tildes");
        assertEquals("Atlético Nacional", liga.equipos().get(0).nombre(), "conserva el primer nombre original");
        assertEquals("https://escudos/nuevo.png", liga.equipos().get(0).logoUrl(), "actualiza el escudo");
    }

    @Test
    void debe_tolerar_fallo_de_la_fuente_de_equipos_y_guardar_igualmente() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("Colombia", "/colombia/", "81", "CO", "Sudamérica", true)));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, null)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais(anyString(), anyInt())).thenReturn(List.of(
                new LigaFuente("Liga", "League", "", null, "https://url-liga", "2026/2027")));
        when(proveedorEquiposPorLiga.obtenerEquipos(anyString(), anyString()))
                .thenThrow(new RuntimeException("scraper caído"));
        when(ligaRepository.buscarPorUrlSoccerway(anyString())).thenReturn(Optional.empty());

        casoDeUso.ejecutar();

        // El fallo de #6 NO aborta: la liga se guarda sin equipos y el catálogo sigue.
        verify(ligaRepository).guardar(any(Liga.class));
        verify(cacheLecturas).eliminar(argThat((String k) -> k.startsWith("equipos:")));
    }

    @Test
    void debe_invalidar_el_cache_de_paises_antes_de_sincronizar() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true)));
        when(paisRepository.buscarPorIsoAlpha2(anyString())).thenReturn(Optional.empty());
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of());

        casoDeUso.ejecutar();

        verify(cacheLecturas).eliminar("paises");
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
