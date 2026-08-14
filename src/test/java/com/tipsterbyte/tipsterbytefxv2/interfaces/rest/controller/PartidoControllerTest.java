package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PartidoControllerTest {

    @Mock
    private RegistrarResultadoUseCase registrarResultadoUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PartidoController(registrarResultadoUseCase))
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
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("El resultado ya fue registrado y no se modifica (BR-003)"));
    }
}