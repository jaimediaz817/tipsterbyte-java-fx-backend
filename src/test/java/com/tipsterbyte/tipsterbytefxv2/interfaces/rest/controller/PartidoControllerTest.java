// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de PartidoController (MockMvc standalone): cubre CU-05
//        y los nuevos endpoints GET de consulta (por liga, fecha, próximos, cuotas).
// [POR QUÉ]: Valida el contrato HTTP sin levantar el contexto Spring: mapeo de DTOs,
//            códigos de estado, y manejo de DomainException → 422.
// [RELACIONES]: PartidoController → RegistrarResultadoUseCase, PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.RegistrarResultadoRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartidoControllerTest {

    @Mock
    private RegistrarResultadoUseCase registrarResultadoUseCase;
    @Mock
    private PartidoRepository partidoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PartidoController(registrarResultadoUseCase, partidoRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_registrar_resultado_y_devolver_204() throws Exception {
        UUID partidoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/partidos/{id}/resultado", partidoId)
                        .contentType("application/json")
                        .content("""
                                {"golesLocal": 2, "golesVisitante": 1}"""))
                .andExpect(status().isNoContent());

        ArgumentCaptor<Resultado> captor = ArgumentCaptor.forClass(Resultado.class);
        verify(registrarResultadoUseCase).ejecutar(eq(partidoId), captor.capture());
        assertEquals(2, captor.getValue().golesLocal());
        assertEquals(1, captor.getValue().golesVisitante());
    }

    @Test
    void debe_devolver_400_cuando_falta_campo_obligatorio() throws Exception {
        mockMvc.perform(post("/api/v1/partidos/{id}/resultado", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"golesLocal": 2}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debe_devolver_422_cuando_el_resultado_ya_fue_registrado() throws Exception {
        UUID partidoId = UUID.randomUUID();
        doThrow(new DomainException("El resultado ya fue registrado y no se modifica (BR-003)"))
                .when(registrarResultadoUseCase).ejecutar(eq(partidoId), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/partidos/{id}/resultado", partidoId)
                        .contentType("application/json")
                        .content("""
                                {"golesLocal": 2, "golesVisitante": 1}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El resultado ya fue registrado y no se modifica (BR-003)"));
    }

    @Test
    void debe_listar_partidos_por_liga() throws Exception {
        UUID ligaId = UUID.randomUUID();
        Partido partido = unPartidoProgramado(ligaId);
        when(partidoRepository.buscarPorLiga(ligaId)).thenReturn(List.of(partido));

        mockMvc.perform(get("/api/v1/partidos").param("ligaId", ligaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(partido.id().toString()))
                .andExpect(jsonPath("$[0].equipoLocal").value("Arsenal"))
                .andExpect(jsonPath("$[0].equipoVisitante").value("Chelsea"))
                .andExpect(jsonPath("$[0].jornada").value(4));
    }

    @Test
    void debe_listar_partidos_por_liga_y_fecha() throws Exception {
        UUID ligaId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 8, 15);
        Partido partido = unPartidoProgramado(ligaId);
        when(partidoRepository.buscarPorLigaYFecha(ligaId, fecha)).thenReturn(List.of(partido));

        mockMvc.perform(get("/api/v1/partidos")
                        .param("ligaId", ligaId.toString())
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(partido.id().toString()));
    }

    @Test
    void debe_listar_proximos_partidos_por_liga() throws Exception {
        UUID ligaId = UUID.randomUUID();
        Partido partido = unPartidoProgramado(ligaId);
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partido));

        mockMvc.perform(get("/api/v1/partidos")
                        .param("ligaId", ligaId.toString())
                        .param("proximos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADO"));
    }

    @Test
    void debe_obtener_cuotas_de_partido() throws Exception {
        UUID partidoId = UUID.randomUUID();
        Partido partido = unPartidoConCuotas(partidoId);
        when(partidoRepository.buscarPorId(partidoId)).thenReturn(Optional.of(partido));

        mockMvc.perform(get("/api/v1/partidos/{id}/cuotas", partidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mercado").value("UNO_X_DOS"))
                .andExpect(jsonPath("$[0].valor").value(1.85));
    }

    @Test
    void debe_devolver_422_cuando_partido_de_cuotas_no_existe() throws Exception {
        UUID partidoId = UUID.randomUUID();
        when(partidoRepository.buscarPorId(partidoId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/partidos/{id}/cuotas", partidoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Partido no encontrado: " + partidoId));
    }

    private Partido unPartidoProgramado(UUID ligaId) {
        return Partido.reconstruir(
                UUID.randomUUID(), ligaId,
                new Equipo(UUID.randomUUID(), "Arsenal"),
                new Equipo(UUID.randomUUID(), "Chelsea"),
                new FechaProgramada(LocalDateTime.of(2026, 8, 15, 15, 0)),
                EstadoPartido.PROGRAMADO,
                List.of(), null, 4);
    }

    private Partido unPartidoConCuotas(UUID partidoId) {
        return Partido.reconstruir(
                partidoId, UUID.randomUUID(),
                new Equipo(UUID.randomUUID(), "Arsenal"),
                new Equipo(UUID.randomUUID(), "Chelsea"),
                new FechaProgramada(LocalDateTime.of(2026, 8, 15, 15, 0)),
                EstadoPartido.PROGRAMADO,
                List.of(new Cuota(Mercado.UNO_X_DOS, new BigDecimal("1.85"))),
                null, 4);
    }
}
