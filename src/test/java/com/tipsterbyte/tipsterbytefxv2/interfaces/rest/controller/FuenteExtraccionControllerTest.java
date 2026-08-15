package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarFuenteExtraccionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FuenteExtraccionControllerTest {

    @Mock
    private GestionarFuenteExtraccionUseCase gestionarFuenteUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FuenteExtraccionController(gestionarFuenteUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_registrar_fuente_y_devolver_201() throws Exception {
        mockMvc.perform(post("/api/v1/fuentes")
                        .contentType("application/json")
                        .content("""
                                {"nombre": "Posiciones Flashscore", "tipo": "STANDINGS", "activa": true}"""))
                .andExpect(status().isCreated());

        verify(gestionarFuenteUseCase).registrarFuente(any());
    }

    @Test
    void debe_devolver_400_si_falta_nombre() throws Exception {
        mockMvc.perform(post("/api/v1/fuentes")
                        .contentType("application/json")
                        .content("""
                                {"tipo": "STANDINGS", "activa": true}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debe_listar_fuentes_del_catalogo() throws Exception {
        FuenteExtraccion standings = new FuenteExtraccion(
                UUID.randomUUID(), "Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);
        when(gestionarFuenteUseCase.listarFuentes()).thenReturn(List.of(standings));

        mockMvc.perform(get("/api/v1/fuentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Posiciones Flashscore"))
                .andExpect(jsonPath("$[0].tipo").value("STANDINGS"))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    @Test
    void debe_asociar_url_de_fuente_a_liga_y_devolver_204() throws Exception {
        UUID ligaId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/ligas/{ligaId}/fuentes/{tipo}", ligaId, "ODDS_WPLAY")
                        .contentType("application/json")
                        .content("""
                                {"tipo": "ODDS_WPLAY", "url": "https://wplay.co/ligas", "activa": true}"""))
                .andExpect(status().isNoContent());

        verify(gestionarFuenteUseCase).asociarUrlFuente(any());
    }

    @Test
    void debe_listar_fuentes_de_una_liga() throws Exception {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion standings = new FuenteExtraccion(
                UUID.randomUUID(), "Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);
        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(
                UUID.randomUUID(), ligaId, standings, "https://flashscore.com/tabla", true);
        when(gestionarFuenteUseCase.listarDetallesDeLiga(ligaId)).thenReturn(List.of(detalle));

        mockMvc.perform(get("/api/v1/ligas/{ligaId}/fuentes", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://flashscore.com/tabla"))
                .andExpect(jsonPath("$[0].tipo").value("STANDINGS"));
    }
}
