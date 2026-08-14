package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PronosticoPublicoDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.SeleccionPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarPronosticosUseCaseTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private PronosticoRepository pronosticoRepository;

    private ConsultarPronosticosUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new ConsultarPronosticosUseCase(suscripcionRepository, partidoRepository, pronosticoRepository);
    }

    @Test
    void debe_consultar_pronosticos_de_tipsters_suscritos_br006() {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterSuscrito = UUID.randomUUID();
        UUID tipsterNoSuscrito = UUID.randomUUID();
        UUID ligaId = UUID.randomUUID();
        LocalDateTime momento = LocalDateTime.of(2026, 3, 1, 12, 0);

        Suscripcion suscripcion = new Suscripcion(clienteId, tipsterSuscrito,
                new Plan("Pro", new BigDecimal("9.99"), 30), momento.minusDays(1));
        when(suscripcionRepository.buscarActivasPorCliente(clienteId)).thenReturn(List.of(suscripcion));

        Partido partido = new Partido(ligaId, new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(momento.plusHours(8)));
        when(partidoRepository.buscarPorLigaYFecha(ligaId, LocalDate.of(2026, 3, 1)))
                .thenReturn(List.of(partido));

        Pronostico publicoSuscrito = new Pronostico(tipsterSuscrito, partido.id(),
                new SeleccionPronostico(Mercado.UNO_X_DOS, "1"), new Cuota(new BigDecimal("1.85")));
        publicoSuscrito.publicar(true, true);
        Pronostico publicoNoSuscrito = new Pronostico(tipsterNoSuscrito, partido.id(),
                new SeleccionPronostico(Mercado.UNO_X_DOS, "2"), new Cuota(new BigDecimal("2.40")));
        publicoNoSuscrito.publicar(true, true);
        when(pronosticoRepository.buscarPublicadosPorPartidos(List.of(partido.id())))
                .thenReturn(List.of(publicoSuscrito, publicoNoSuscrito));

        List<PronosticoPublicoDto> resultado = casoDeUso.ejecutar(clienteId, ligaId,
                LocalDate.of(2026, 3, 1), momento);

        assertEquals(1, resultado.size());
        PronosticoPublicoDto dto = resultado.get(0);
        assertEquals(tipsterSuscrito, dto.tipsterId());
        assertEquals("Real Madrid", dto.equipoLocal());
        assertEquals(Mercado.UNO_X_DOS, dto.mercado());
        assertEquals("1", dto.resultadoEsperado());
        assertEquals(new BigDecimal("1.85"), dto.cuotaValor());
    }

    @Test
    void debe_excluir_suscripciones_expiradas_br006() {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterExpirado = UUID.randomUUID();
        UUID ligaId = UUID.randomUUID();
        LocalDateTime momento = LocalDateTime.of(2026, 3, 1, 12, 0);

        Suscripcion expirada = new Suscripcion(clienteId, tipsterExpirado,
                new Plan("Pro", new BigDecimal("9.99"), 1), momento.minusDays(10));
        when(suscripcionRepository.buscarActivasPorCliente(clienteId)).thenReturn(List.of(expirada));

        Partido partido = new Partido(ligaId, new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(momento.plusHours(8)));
        when(partidoRepository.buscarPorLigaYFecha(ligaId, LocalDate.of(2026, 3, 1)))
                .thenReturn(List.of(partido));

        Pronostico pronostico = new Pronostico(tipsterExpirado, partido.id(),
                new SeleccionPronostico(Mercado.UNO_X_DOS, "1"), new Cuota(new BigDecimal("1.85")));
        pronostico.publicar(true, true);
        when(pronosticoRepository.buscarPublicadosPorPartidos(List.of(partido.id())))
                .thenReturn(List.of(pronostico));

        List<PronosticoPublicoDto> resultado = casoDeUso.ejecutar(clienteId, ligaId,
                LocalDate.of(2026, 3, 1), momento);

        assertTrue(resultado.isEmpty());
    }
}