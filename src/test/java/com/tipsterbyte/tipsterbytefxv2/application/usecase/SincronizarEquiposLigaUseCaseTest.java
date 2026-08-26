// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios de CU-16 (SincronizarEquiposLigaUseCase): ruta rápida sobre
//        plantilla existente (HU-FRONT-05), forzado ?forzar=true, flujo normal de
//        poblamiento y anti-solapamiento por liga (409).
// [POR QUÉ]: La ruta rápida evita re-scrapear Python (~minutos) cuando la BD ya tiene
//            los equipos; los tests fijan ese contrato y el guard de concurrencia.
// [RELACIONES]: CU-16 → ProveedorEquiposPorLiga (#6 mock) + CacheLecturas + LigaRepository
//               (mocks Mockito, sin Spring ni red).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizarEquiposLigaUseCaseTest {

    private ProveedorEquiposPorLiga proveedorEquiposPorLiga;
    private CacheLecturas cacheLecturas;
    private LigaRepository ligaRepository;
    private SincronizarEquiposLigaUseCase casoDeUso;
    private Liga liga;
    private UUID ligaId;

    @BeforeEach
    void setUp() {
        proveedorEquiposPorLiga = mock(ProveedorEquiposPorLiga.class);
        cacheLecturas = mock(CacheLecturas.class);
        ligaRepository = mock(LigaRepository.class);
        casoDeUso = new SincronizarEquiposLigaUseCase(
                proveedorEquiposPorLiga, cacheLecturas, ligaRepository);

        liga = new Liga("Liga Profesional", "Argentina",
                "https://co.soccerway.com/argentina/liga-profesional/", null);
        ligaId = liga.id();
        liga.addTemporada(new Temporada(liga.id(), "2026/2027", null,
                2026, 2027, EstadoTemporada.PLANIFICADA));
    }

    private void equipoEnPlantilla(String nombre) {
        // Usa el método del aggregate (getTemporadas() devuelve copia inmutable)
        liga.agregarEquipo(new Equipo(nombre, "https://escudo.png"));
    }

    private List<EquipoFuente> dosEquiposFuente() {
        return List.of(
                new EquipoFuente("River Plate", "https://river.png"),
                new EquipoFuente("Boca Juniors", "https://boca.png"));
    }

    @Test
    void debe_devolver_plantilla_existente_sin_consultar_fuente_cuando_no_se_fuerza() {
        equipoEnPlantilla("River Plate");
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(liga));

        var resultado = casoDeUso.ejecutar(ligaId);

        assertTrue(resultado.desdePlantillaExistente());
        assertEquals(0, resultado.creados());
        assertEquals(0, resultado.actualizados());
        assertEquals(1, resultado.totalPlantilla());
        verify(proveedorEquiposPorLiga, never()).obtenerEquipos(anyString(), anyString());
        verify(cacheLecturas, never()).eliminar(anyString());
        verify(ligaRepository, never()).guardar(org.mockito.ArgumentMatchers.any(Liga.class));
    }

    @Test
    void debe_forzar_scrape_cuando_forzar_true_aunque_la_plantilla_exista() {
        equipoEnPlantilla("River Plate");
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(liga));
        when(proveedorEquiposPorLiga.obtenerEquipos("Argentina", "Liga Profesional"))
                .thenReturn(dosEquiposFuente());

        var resultado = casoDeUso.ejecutar(ligaId, true);

        assertEquals(false, resultado.desdePlantillaExistente());
        // "River Plate" ya estaba (match normalizado) y "Boca Juniors" es nuevo
        assertEquals(1, resultado.creados());
        assertEquals(1, resultado.actualizados());
        assertEquals(2, resultado.totalPlantilla());
        verify(cacheLecturas).eliminar(CacheClaves.equipos("Argentina", "Liga Profesional"));
        verify(proveedorEquiposPorLiga).obtenerEquipos("Argentina", "Liga Profesional");
        verify(ligaRepository).guardar(liga);
    }

    @Test
    void debe_poblar_normalmente_cuando_la_plantilla_esta_vacia() {
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(liga));
        when(proveedorEquiposPorLiga.obtenerEquipos("Argentina", "Liga Profesional"))
                .thenReturn(dosEquiposFuente());

        var resultado = casoDeUso.ejecutar(ligaId);

        assertEquals(false, resultado.desdePlantillaExistente());
        assertEquals(2, resultado.creados());
        assertEquals(2, resultado.totalPlantilla());
        verify(proveedorEquiposPorLiga).obtenerEquipos("Argentina", "Liga Profesional");
        verify(ligaRepository).guardar(liga);
    }

    @Test
    void debe_bloquear_segunda_ejecucion_mientras_hay_una_en_curso() {
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(liga));
        when(proveedorEquiposPorLiga.obtenerEquipos("Argentina", "Liga Profesional"))
                .thenAnswer(invocacion -> {
                    // Simula un segundo click mientras el primero sigue scrapeando
                    assertThrows(PoblamientoEnCursoException.class,
                            () -> casoDeUso.ejecutar(ligaId));
                    return dosEquiposFuente();
                });

        var resultado = casoDeUso.ejecutar(ligaId);

        assertEquals(false, resultado.desdePlantillaExistente());
        assertEquals(2, resultado.totalPlantilla());
    }

    @Test
    void debe_liberar_el_flag_tras_un_error_para_permitir_reintento() {
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(liga));
        when(proveedorEquiposPorLiga.obtenerEquipos("Argentina", "Liga Profesional"))
                .thenThrow(new RuntimeException("scraper caído"))
                .thenReturn(dosEquiposFuente());

        assertThrows(RuntimeException.class, () -> casoDeUso.ejecutar(ligaId, true));
        var reintento = casoDeUso.ejecutar(ligaId, true);

        assertEquals(2, reintento.totalPlantilla());
    }
}
