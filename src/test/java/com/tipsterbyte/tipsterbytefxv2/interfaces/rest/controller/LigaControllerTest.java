package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.DisponibilidadFuentes;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LigaControllerTest {

    @Mock
    private ActivarLigaUseCase activarLigaUseCase;
    @Mock
    private SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    @Mock
    private SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    @Mock
    private SincronizarCuotasUseCase sincronizarCuotasUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new LigaController(activarLigaUseCase, sincronizarPosicionesUseCase,
                        sincronizarCalendarioUseCase, sincronizarCuotasUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_activar_liga_y_devolver_204() throws Exception {
        UUID ligaId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ligas/{id}/activacion", ligaId)
                        .contentType("application/json")
                        .content("""
                                {"posiciones": true, "calendario": true, "cuotas": true}"""))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DisponibilidadFuentes> captor = ArgumentCaptor.forClass(DisponibilidadFuentes.class);
        verify(activarLigaUseCase).ejecutar(eq(ligaId), captor.capture());
        assertEquals(true, captor.getValue().posiciones());
        assertEquals(true, captor.getValue().calendario());
        assertEquals(true, captor.getValue().cuotas());
    }

    @Test
    void debe_devolver_422_cuando_la_liga_no_existe() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(activarLigaUseCase.ejecutar(eq(ligaId), any(DisponibilidadFuentes.class)))
                .thenThrow(new DomainException("Liga no encontrada: " + ligaId));

        mockMvc.perform(post("/api/v1/ligas/{id}/activacion", ligaId)
                        .contentType("application/json")
                        .content("""
                                {"posiciones": true, "calendario": true, "cuotas": true}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("Liga no encontrada: " + ligaId));
    }

    @Test
    void debe_devolver_400_cuando_falta_campo_obligatorio() throws Exception {
        mockMvc.perform(post("/api/v1/ligas/{id}/activacion", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"posiciones": true}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debe_sincronizar_posiciones_y_devolver_contador() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(sincronizarPosicionesUseCase.ejecutar(ligaId)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ligas/{id}/sincronizaciones/posiciones", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventosEmitidos").value(0));
    }

    @Test
    void debe_sincronizar_calendario_y_devolver_contador() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(sincronizarCalendarioUseCase.ejecutar(ligaId)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ligas/{id}/sincronizaciones/calendario", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventosEmitidos").value(0));
    }

    @Test
    void debe_sincronizar_cuotas_y_devolver_contador() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(sincronizarCuotasUseCase.ejecutar(ligaId)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ligas/{id}/sincronizaciones/cuotas", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventosEmitidos").value(0));
    }
}