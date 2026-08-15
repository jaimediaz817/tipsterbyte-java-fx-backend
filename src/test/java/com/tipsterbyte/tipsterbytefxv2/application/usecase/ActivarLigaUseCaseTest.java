package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ActivarLigaUseCase casoDeUso;

    private FuenteExtraccion fuenteStandings;
    private FuenteExtraccion fuenteCalendar;
    private FuenteExtraccion fuenteOddsWplay;

    @BeforeEach
    void setUp() {
        casoDeUso = new ActivarLigaUseCase(ligaRepository, fuenteRepository, detalleRepository);
        fuenteStandings = new FuenteExtraccion("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);
        fuenteCalendar = new FuenteExtraccion("Calendario Soccerway", TipoFuenteExtraccion.CALENDAR, true);
        fuenteOddsWplay = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
    }

    @Test
    void debe_activar_liga_y_emitir_liga_activada_br001() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
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
        // Una URL por fuente: 3 detalles guardados.
        verify(detalleRepository, times(3)).guardar(any(DetalleFuenteExtraccion.class));
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("LigaActivada")));
    }

    @Test
    void debe_rechazar_activacion_sin_fuentes_operativas_br001() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        // Falta la URL de cuotas → disponibilidad false → BR-001 violado.
        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id(),
                new ActivarLigaComando("https://flashscore.com/tabla", "https://soccerway.com/calendario", null)));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(ligaRepository, never()).guardar(any());
        verify(detalleRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_activacion_si_no_existe_fuente_registrada() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
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
}
