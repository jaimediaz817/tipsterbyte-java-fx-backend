package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarCalendarioUseCaseTest {

    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private ProveedorCalendario proveedorCalendario;

    private SincronizarCalendarioUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new SincronizarCalendarioUseCase(ligaRepository, partidoRepository, proveedorCalendario);
    }

    private Liga ligaActiva() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        liga.activar(true, true, true);
        return liga;
    }

    @Test
    void debe_crear_partidos_y_emitir_partido_programado() {
        Liga liga = ligaActiva();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorCalendario.obtenerCalendario(liga.id())).thenReturn(List.of(
                new PartidoFuente("Real Madrid", "FC Barcelona",
                        LocalDateTime.of(2026, 3, 1, 20, 0))));

        List<DomainEvent> eventos = casoDeUso.ejecutar(liga.id());

        verify(partidoRepository).guardar(any());
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("PartidoProgramado")));
        assertEquals(2, liga.equipos().size());
    }

    @Test
    void debe_rechazar_calendario_si_liga_inactiva() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id()));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(proveedorCalendario, never()).obtenerCalendario(any());
        verify(partidoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_si_liga_no_existe() {
        UUID id = UUID.randomUUID();
        when(ligaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(id));
        verify(partidoRepository, never()).guardar(any());
    }
}