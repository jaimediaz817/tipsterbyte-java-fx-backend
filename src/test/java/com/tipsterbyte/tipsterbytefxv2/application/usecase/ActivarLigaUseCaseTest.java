package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.DisponibilidadFuentes;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
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
class ActivarLigaUseCaseTest {

    @Mock
    private LigaRepository ligaRepository;

    private ActivarLigaUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new ActivarLigaUseCase(ligaRepository);
    }

    @Test
    void debe_activar_liga_y_emitir_liga_activada_br001() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        List<DomainEvent> eventos = casoDeUso.ejecutar(liga.id(),
                new DisponibilidadFuentes(true, true, true));

        assertEquals(EstadoLiga.ACTIVA, liga.estado());
        verify(ligaRepository).guardar(liga);
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("LigaActivada")));
    }

    @Test
    void debe_rechazar_activacion_sin_fuentes_operativas_br001() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id(),
                new DisponibilidadFuentes(true, true, false)));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_si_liga_no_existe() {
        UUID id = UUID.randomUUID();
        when(ligaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(id,
                new DisponibilidadFuentes(true, true, true)));
        verify(ligaRepository, never()).guardar(any());
    }
}