// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de PaisInteresController (MockMvc standalone): cubre CU-14
//        (POST/GET/DELETE/PUT de países de interés) y el manejo de errores 400/422.
// [POR QUÉ]: Valida el contrato HTTP del recurso de preferencia de poblamiento sin
//            levantar Spring: códigos de estado, validación del body y DomainException.
// [RELACIONES]: PaisInteresController → GestionarPaisesInteresUseCase (CU-14).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarPaisInteresComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarPaisesInteresUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaisInteresControllerTest {

    @Mock
    private GestionarPaisesInteresUseCase gestionarPaisesInteresUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaisInteresController(gestionarPaisesInteresUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_registrar_pais_de_interes() throws Exception {
        mockMvc.perform(post("/api/v1/paises-interes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isoAlpha2": "CO", "nombre": "Colombia"}"""))
                .andExpect(status().isCreated());

        verify(gestionarPaisesInteresUseCase).registrar(
                new RegistrarPaisInteresComando("CO", "Colombia"));
    }

    @Test
    void debe_devolver_400_cuando_falta_iso_alpha2() throws Exception {
        mockMvc.perform(post("/api/v1/paises-interes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isoAlpha2": " ", "nombre": "Colombia"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void debe_listar_paises_de_interes_por_prioridad() throws Exception {
        when(gestionarPaisesInteresUseCase.listar()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1),
                new PaisInteres("ES", "España", 2)));

        mockMvc.perform(get("/api/v1/paises-interes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isoAlpha2").value("CO"))
                .andExpect(jsonPath("$[0].prioridad").value(1))
                .andExpect(jsonPath("$[1].isoAlpha2").value("ES"))
                .andExpect(jsonPath("$[1].prioridad").value(2));
    }

    @Test
    void debe_eliminar_pais_de_interes() throws Exception {
        mockMvc.perform(delete("/api/v1/paises-interes/{isoAlpha2}", "CO"))
                .andExpect(status().isNoContent());

        verify(gestionarPaisesInteresUseCase).eliminar("CO");
    }

    @Test
    void debe_reemplazar_preferencias_completas() throws Exception {
        mockMvc.perform(put("/api/v1/paises-interes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"isoAlpha2": "CO", "nombre": "Colombia"},
                                 {"isoAlpha2": "ES", "nombre": "España"}]"""))
                .andExpect(status().isNoContent());

        verify(gestionarPaisesInteresUseCase).reemplazarPreferencias(List.of(
                new RegistrarPaisInteresComando("CO", "Colombia"),
                new RegistrarPaisInteresComando("ES", "España")));
    }

    @Test
    void debe_devolver_422_cuando_el_pais_no_esta_disponible() throws Exception {
        doThrow(new DomainException("El país no está disponible en la fuente de países: XX"))
                .when(gestionarPaisesInteresUseCase).registrar(
                        new RegistrarPaisInteresComando("XX", "No existe"));

        mockMvc.perform(post("/api/v1/paises-interes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isoAlpha2": "XX", "nombre": "No existe"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }
}