// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-11 (GestionarFuenteExtraccionUseCase) con asociación de
//        URLs por temporada vigente de la liga.
// [POR QUÉ]: Verifica el catálogo de fuentes (registrar sin duplicar tipo, listar) y la
//            asociación de URLs resolviendo la temporada activa (o primera registrada)
//            de la liga, actualizando en lugar de duplicar.
// [RELACIONES]: HU-11 → CU-11 → FuenteExtraccionRepository + DetalleFuenteExtraccionRepository
//               + TemporadaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AsociarUrlFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarFuenteExtraccionUseCaseTest {

    @Mock
    private FuenteExtraccionRepository fuenteRepository;
    @Mock
    private DetalleFuenteExtraccionRepository detalleRepository;
    @Mock
    private TemporadaRepository temporadaRepository;

    private GestionarFuenteExtraccionUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new GestionarFuenteExtraccionUseCase(fuenteRepository, detalleRepository,
                temporadaRepository);
    }

    private Temporada temporadaDe(UUID ligaId) {
        return new Temporada(ligaId, "2025/2026", null, 2025, 2026, EstadoTemporada.PLANIFICADA);
    }

    @Test
    void debe_registrar_fuente_nueva() {
        casoDeUso.registrarFuente(new RegistrarFuenteComando("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true));

        ArgumentCaptor<FuenteExtraccion> captor = ArgumentCaptor.forClass(FuenteExtraccion.class);
        verify(fuenteRepository).guardar(captor.capture());
        assertEquals(TipoFuenteExtraccion.STANDINGS, captor.getValue().tipo());
    }

    @Test
    void debe_rechazar_tipo_ya_registrado() {
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.of(new FuenteExtraccion("Existente", TipoFuenteExtraccion.STANDINGS, true)));

        assertThrows(DomainException.class, () -> casoDeUso.registrarFuente(
                new RegistrarFuenteComando("Duplicada", TipoFuenteExtraccion.STANDINGS, true)));
        verify(fuenteRepository, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debe_listar_fuentes_del_catalogo() {
        when(fuenteRepository.buscarTodas()).thenReturn(List.of(
                new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true),
                new FuenteExtraccion("Calendario", TipoFuenteExtraccion.CALENDAR, true)));

        List<FuenteExtraccion> fuentes = casoDeUso.listarFuentes();

        assertEquals(2, fuentes.size());
    }

    @Test
    void debe_asociar_url_de_fuente_a_la_temporada_vigente_de_la_liga() {
        UUID ligaId = UUID.randomUUID();
        Temporada temporada = temporadaDe(ligaId);
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY)).thenReturn(Optional.of(fuente));
        when(temporadaRepository.buscarActivaPorLigaId(ligaId)).thenReturn(Optional.of(temporada));

        casoDeUso.asociarUrlFuente(new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "https://wplay.co/ligas", true));

        ArgumentCaptor<DetalleFuenteExtraccion> captor = ArgumentCaptor.forClass(DetalleFuenteExtraccion.class);
        verify(detalleRepository).guardar(captor.capture());
        assertEquals(temporada.id(), captor.getValue().temporadaId());
        assertEquals("https://wplay.co/ligas", captor.getValue().url());
    }

    @Test
    void debe_actualizar_url_existente_en_lugar_de_duplicar() {
        UUID ligaId = UUID.randomUUID();
        Temporada temporada = temporadaDe(ligaId);
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        DetalleFuenteExtraccion existente = new DetalleFuenteExtraccion(
                UUID.randomUUID(), temporada.id(), fuente, "https://wplay.co/vieja", true);
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY)).thenReturn(Optional.of(fuente));
        when(temporadaRepository.buscarActivaPorLigaId(ligaId)).thenReturn(Optional.of(temporada));
        when(detalleRepository.buscarPorTemporadaYTipo(temporada.id(), TipoFuenteExtraccion.ODDS_WPLAY))
                .thenReturn(Optional.of(existente));

        casoDeUso.asociarUrlFuente(new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "https://wplay.co/nueva", true));

        ArgumentCaptor<DetalleFuenteExtraccion> captor = ArgumentCaptor.forClass(DetalleFuenteExtraccion.class);
        verify(detalleRepository).guardar(captor.capture());
        assertEquals(existente.id(), captor.getValue().id());
        assertEquals("https://wplay.co/nueva", captor.getValue().url());
    }

    @Test
    void debe_rechazar_asociacion_si_no_existe_fuente_del_tipo() {
        UUID ligaId = UUID.randomUUID();
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.CALENDAR)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.asociarUrlFuente(
                new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.CALENDAR, "https://soccerway.com/cal", true)));
    }

    @Test
    void debe_rechazar_asociacion_si_liga_no_tiene_temporadas() {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY)).thenReturn(Optional.of(fuente));
        when(temporadaRepository.buscarActivaPorLigaId(ligaId)).thenReturn(Optional.empty());
        when(temporadaRepository.buscarPorLigaId(ligaId)).thenReturn(List.of());

        assertThrows(DomainException.class, () -> casoDeUso.asociarUrlFuente(
                new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "https://wplay.co/ligas", true)));
    }

    @Test
    void debe_listar_detalles_de_liga() {
        UUID ligaId = UUID.randomUUID();
        when(detalleRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                new DetalleFuenteExtraccion(UUID.randomUUID(),
                        new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true),
                        "https://flashscore.com/tabla", true)));

        List<DetalleFuenteExtraccion> detalles = casoDeUso.listarDetallesDeLiga(ligaId);

        assertEquals(1, detalles.size());
    }
}
