package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CrearPronosticoComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PronosticoPublicoDto;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarPronosticosUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.PublicarPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PronosticoControllerTest {

    @Mock
    private CrearPronosticoUseCase crearPronosticoUseCase;
    @Mock
    private PublicarPronosticoUseCase publicarPronosticoUseCase;
    @Mock
    private ConsultarPronosticosUseCase consultarPronosticosUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PronosticoController(crearPronosticoUseCase, publicarPronosticoUseCase,
                        consultarPronosticosUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_crear_pronostico_y_devolver_201_con_id_y_location() throws Exception {
        UUID tipsterId = UUID.randomUUID();
        UUID partidoId = UUID.randomUUID();
        UUID pronosticoId = UUID.randomUUID();
        when(crearPronosticoUseCase.ejecutar(any(CrearPronosticoComando.class))).thenReturn(pronosticoId);

        mockMvc.perform(post("/api/v1/pronosticos")
                        .contentType("application/json")
                        .content("""
                                {"tipsterId": "%s", "partidoId": "%s", "mercado": "UNO_X_DOS",
                                 "resultadoEsperado": "LOCAL", "cuotaValor": 2.5}"""
                                .formatted(tipsterId, partidoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pronosticoId.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Location", "/api/v1/pronosticos/" + pronosticoId));

        verify(crearPronosticoUseCase).ejecutar(any(CrearPronosticoComando.class));
    }

    @Test
    void debe_devolver_400_cuando_falta_campo_obligatorio() throws Exception {
        mockMvc.perform(post("/api/v1/pronosticos")
                        .contentType("application/json")
                        .content("""
                                {"tipsterId": "%s"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debe_publicar_pronostico_y_devolver_204() throws Exception {
        UUID pronosticoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/pronosticos/{id}/publicacion", pronosticoId))
                .andExpect(status().isNoContent());

        verify(publicarPronosticoUseCase).ejecutar(pronosticoId);
    }

    @Test
    void debe_consultar_pronosticos_por_liga_y_fecha() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID ligaId = UUID.randomUUID();
        UUID partidoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 8, 20);

        PronosticoPublicoDto dto = new PronosticoPublicoDto(
                UUID.randomUUID(), UUID.randomUUID(), partidoId,
                "Local FC", "Visitante FC", LocalDateTime.of(2026, 8, 20, 18, 0),
                Mercado.UNO_X_DOS, "LOCAL", new BigDecimal("2.5"));
        when(consultarPronosticosUseCase.ejecutar(eq(clienteId), eq(ligaId), eq(fecha), any(LocalDateTime.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/pronosticos")
                        .param("clienteId", clienteId.toString())
                        .param("ligaId", ligaId.toString())
                        .param("fecha", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoLocal").value("Local FC"))
                .andExpect(jsonPath("$[0].mercado").value("UNO_X_DOS"));
    }

    @Test
    void debe_devolver_400_cuando_faltan_params_de_consulta() throws Exception {
        mockMvc.perform(get("/api/v1/pronosticos")
                        .param("clienteId", UUID.randomUUID().toString())
                        .param("ligaId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }
}