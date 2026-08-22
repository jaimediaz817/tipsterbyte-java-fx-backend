// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-04 (ActivarLigaUseCase) con asociación de URLs por temporada.
// [POR QUÉ]: Verifica BR-001 (activación solo con las 3 fuentes operativas), la creación
//            de DetalleFuenteExtraccion sobre la temporada vigente (activa o primera
//            registrada) y los errores: liga inexistente, fuente no registrada, liga sin
//            temporadas.
// [RELACIONES]: HU-04 → CU-04 → LigaRepository + FuenteExtraccionRepository +
//               DetalleFuenteExtraccionRepository + TemporadaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivarLigaUseCaseTest {

    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private FuenteExtraccionRepository fuenteRepository;
    @Mock
    private DetalleFuenteExtraccionRepository detalleRepository;
    @Mock
    private TemporadaRepository temporadaRepository;

    private ActivarLigaUseCase casoDeUso;

    private FuenteExtraccion fuenteStandings;
    private FuenteExtraccion fuenteCalendar;
    private FuenteExtraccion fuenteOddsWplay;

    @BeforeEach
    void setUp() {
        casoDeUso = new ActivarLigaUseCase(ligaRepository, fuenteRepository, detalleRepository,
                temporadaRepository);
        fuenteStandings = new FuenteExtraccion("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);
        fuenteCalendar = new FuenteExtraccion("Calendario Soccerway", TipoFuenteExtraccion.CALENDAR, true);
        fuenteOddsWplay = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
    }

    private Liga ligaConTemporada() {
        Liga liga = new Liga("La Liga", "España");
        liga.addTemporada(new Temporada(liga.id(), "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA));
        return liga;
    }

    @Test
    void debe_activar_liga_y_emitir_liga_activada_br001() {
        Liga liga = ligaConTemporada();
        UUID temporadaId = liga.getTemporadas().iterator().next().id();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(temporadaRepository.buscarActivaPorLigaId(liga.id())).thenReturn(Optional.empty());
        when(temporadaRepository.buscarPorLigaId(liga.id()))
                .thenReturn(List.of(liga.getTemporadas().iterator().next()));
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.of(fuenteStandings));
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.CALENDAR))
                .thenReturn(Optional.of(fuenteCalendar));
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY))
                .thenReturn(Optional.of(fuenteOddsWplay));

        List<DomainEvent> eventos = casoDeUso.ejecutar(liga.id(),
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", "https://wplay.co/ligas"));

        assertEquals(EstadoLiga.ACTIVA, liga.estado());
        verify(ligaRepository).guardar(liga);
        // Una URL por fuente: 3 detalles guardados, todos sobre la temporada vigente.
        ArgumentCaptor<DetalleFuenteExtraccion> captor = ArgumentCaptor.forClass(DetalleFuenteExtraccion.class);
        verify(detalleRepository, times(3)).guardar(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .allMatch(d -> d.temporadaId().equals(temporadaId)));
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("LigaActivada")));
    }

    @Test
    void debe_rechazar_activacion_sin_fuentes_operativas_br001() {
        Liga liga = ligaConTemporada();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(temporadaRepository.buscarActivaPorLigaId(liga.id())).thenReturn(Optional.empty());
        when(temporadaRepository.buscarPorLigaId(liga.id()))
                .thenReturn(List.of(liga.getTemporadas().iterator().next()));

        // Falta la URL de cuotas → disponibilidad false → BR-001 violado.
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id(),
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", null)));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(ligaRepository, never()).guardar(any());
        verify(detalleRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_activacion_si_no_existe_fuente_registrada() {
        Liga liga = ligaConTemporada();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(temporadaRepository.buscarActivaPorLigaId(liga.id())).thenReturn(Optional.empty());
        when(temporadaRepository.buscarPorLigaId(liga.id()))
                .thenReturn(List.of(liga.getTemporadas().iterator().next()));
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id(),
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", "https://wplay.co/ligas")));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_si_liga_no_existe() {
        UUID id = UUID.randomUUID();
        when(ligaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(id,
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", "https://wplay.co/ligas")));
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_si_liga_no_tiene_temporadas() {
        Liga liga = new Liga("La Liga", "España");
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(temporadaRepository.buscarActivaPorLigaId(liga.id())).thenReturn(Optional.empty());
        when(temporadaRepository.buscarPorLigaId(liga.id())).thenReturn(List.of());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id(),
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", "https://wplay.co/ligas")));
        verify(ligaRepository, never()).guardar(any());
    }
}
