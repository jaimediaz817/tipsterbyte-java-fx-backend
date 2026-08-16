// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de SuscripcionController (MockMvc standalone): cubre CU-09
//        y el nuevo endpoint GET de consulta de suscripciones activas.
// [POR QUÉ]: Valida el contrato HTTP sin levantar el contexto Spring: creación con 201,
//            listado con 200, validación de request → 400, y autorización de propiedad.
// [RELACIONES]: SuscripcionController → CrearSuscripcionUseCase, SuscripcionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.event.SuscripcionCreada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.CrearSuscripcionRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuscripcionControllerTest {

    @Mock
    private CrearSuscripcionUseCase crearSuscripcionUseCase;
    @Mock
    private SuscripcionRepository suscripcionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SuscripcionController(crearSuscripcionUseCase, suscripcionRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    @Test
    void debe_listar_suscripciones_del_cliente_autenticado() throws Exception {
        UUID clienteId = UUID.randomUUID();
        autenticarComoCliente(clienteId);
        Suscripcion suscripcion = unaSuscripcionActiva(clienteId);
        when(suscripcionRepository.buscarActivasPorCliente(clienteId)).thenReturn(List.of(suscripcion));

        mockMvc.perform(get("/api/v1/suscripciones")
                        .param("clienteId", clienteId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].suscripcionId").value(suscripcion.id().toString()))
                .andExpect(jsonPath("$[0].planNombre").value("Pro"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    void debe_devolver_422_cuando_cliente_consulta_suscripciones_de_otro() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID otroClienteId = UUID.randomUUID();
        autenticarComoCliente(clienteId);

        mockMvc.perform(get("/api/v1/suscripciones")
                        .param("clienteId", otroClienteId.toString()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: solo puedes consultar tus propias suscripciones"));
    }

    private void autenticarComoCliente(UUID clienteId) {
        Usuario usuario = new Usuario(clienteId, "Cliente", new Email("c@example.com"), "hash", Rol.CLIENTE, true,
                LocalDateTime.now());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Suscripcion unaSuscripcionActiva(UUID clienteId) {
        return Suscripcion.reconstruir(
                UUID.randomUUID(), clienteId, UUID.randomUUID(),
                new Plan("Pro", new BigDecimal("9.99"), 30),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 31, 10, 0),
                EstadoSuscripcion.ACTIVA);
    }
}
