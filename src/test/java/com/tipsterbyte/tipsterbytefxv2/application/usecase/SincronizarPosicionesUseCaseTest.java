package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;
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
class SincronizarPosicionesUseCaseTest {

    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private ProveedorPosiciones proveedorPosiciones;
    @Mock
    private CacheLecturas cacheLecturas;

    private SincronizarPosicionesUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new SincronizarPosicionesUseCase(ligaRepository, proveedorPosiciones, cacheLecturas);
    }

    private Liga ligaActiva() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        liga.activar(true, true, true);
        liga.pullEventos();
        return liga;
    }

    @Test
    void debe_sincronizar_posiciones_de_liga_activa() {
        Liga liga = ligaActiva();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorPosiciones.obtenerPosiciones(liga.id())).thenReturn(List.of(
                new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10),
                new PosicionFuente("FC Barcelona", 2, 5, 2, 2, 1, 9, 6, 8)));

        List<DomainEvent> eventos = casoDeUso.ejecutar(liga.id());

        assertEquals(2, liga.posiciones().size());
        PosicionTabla primera = liga.posiciones().get(0);
        assertEquals("Real Madrid", primera.equipo().nombre());
        assertEquals(10, primera.puntos());
        assertEquals(2, liga.equipos().size());
        verify(ligaRepository).guardar(liga);
        assertTrue(eventos.isEmpty());
    }

    @Test
    void debe_persistir_la_racha_de_ultimos_resultados_de_la_fuente() {
        Liga liga = ligaActiva();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorPosiciones.obtenerPosiciones(liga.id())).thenReturn(List.of(
                new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10,
                        List.of(ResultadoReciente.GANADO, ResultadoReciente.EMPATE,
                                ResultadoReciente.PERDIDO, ResultadoReciente.GANADO, ResultadoReciente.GANADO))));

        casoDeUso.ejecutar(liga.id());

        PosicionTabla primera = liga.posiciones().get(0);
        assertEquals(5, primera.ultimosResultados().size());
        assertEquals(ResultadoReciente.GANADO, primera.ultimosResultados().get(0));
    }

    @Test
    void debe_rechazar_sincronizacion_si_liga_no_existe() {
        UUID id = UUID.randomUUID();
        when(ligaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(id));
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_sincronizacion_si_liga_inactiva_br002() {
        Liga liga = new Liga("La Liga", "España", new Temporada(2025, 2026));
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorPosiciones.obtenerPosiciones(liga.id())).thenReturn(List.of(
                new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10)));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(liga.id()));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
        verify(ligaRepository, never()).guardar(any());
    }

    @Test
    void debe_agregar_equipo_nuevo_cuando_no_existe_en_liga() {
        Liga liga = ligaActiva();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorPosiciones.obtenerPosiciones(liga.id())).thenReturn(List.of(
                new PosicionFuente("Atlético", 1, 5, 3, 1, 1, 10, 4, 10)));

        casoDeUso.ejecutar(liga.id());

        assertEquals(1, liga.equipos().size());
        assertEquals("Atlético", liga.equipos().get(0).nombre());
    }

    @Test
    void debe_invalidar_cache_de_posiciones_antes_de_consultar_fuente() {
        Liga liga = ligaActiva();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));
        when(proveedorPosiciones.obtenerPosiciones(liga.id())).thenReturn(List.of(
                new PosicionFuente("Real Madrid", 1, 5, 3, 1, 1, 10, 4, 10)));

        casoDeUso.ejecutar(liga.id());

        verify(cacheLecturas).eliminar("posiciones:" + liga.id());
    }
}