package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.event.SuscripcionCreada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuscripcionControllerTest {

    @Mock
    private CrearSuscripcionUseCase crearSuscripcionUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SuscripcionController(crearSuscripcionUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_crear_suscripcion_y_devolver_201() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterId = UUID.randomUUID();
        UUID suscripcionId = UUID.randomUUID();
        when(crearSuscripcionUseCase.ejecutar(eq(clienteId), eq(tipsterId), any(Plan.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(new SuscripcionCreada(suscripcionId)));

        mockMvc.perform(post("/api/v1/suscripciones")
                        .contentType("application/json")
                        .content("""
                                {"clienteId": "%s", "tipsterId": "%s", "planNombre": "Pro",
                                 "planPrecio": 9.99, "planDuracionDias": 30, "fechaInicio": "2026-08-01T10:00:00"}"""
                                .formatted(clienteId, tipsterId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suscripcionId").value(suscripcionId.toString()))
                .andExpect(jsonPath("$.planNombre").value("Pro"))
                .andExpect(jsonPath("$.estado").value(EstadoSuscripcion.ACTIVA.name()));
    }

    @Test
    void debe_devolver_400_cuando_falta_campo_obligatorio() throws Exception {
        mockMvc.perform(post("/api/v1/suscripciones")
                        .contentType("application/json")
                        .content("""
                                {"clienteId": "%s"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}