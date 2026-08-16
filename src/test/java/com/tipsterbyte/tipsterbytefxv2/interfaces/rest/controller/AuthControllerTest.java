// ─────────────────────────────────────────────
// [QUÉ]: Test del AuthController: registro (CU-12) y login (CU-13) con MockMvc.
// [POR QUÉ]: Verifica el contrato HTTP de autenticación: 201 en registro, 200 con
//            token en login, y 400 en validación de request.
// [RELACIONES]: AuthController → RegistrarUsuarioUseCase + AutenticarUsuarioUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticacionResultado;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.AutenticarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;
    @Mock
    private AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(registrarUsuarioUseCase, autenticarUsuarioUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_registrar_y_devolver_201_con_token() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrarUsuarioUseCase.ejecutar(any(RegistrarUsuarioComando.class))).thenReturn(id);
        when(autenticarUsuarioUseCase.ejecutar(any(AutenticarUsuarioComando.class)))
                .thenReturn(new AutenticacionResultado(id, "Ana", "ana@example.com", Rol.TIPSTER, "jwt.token",
                        LocalDateTime.of(2026, 8, 16, 10, 30)));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType("application/json")
                        .content("""
                                {"nombre": "Ana", "email": "ana@example.com",
                                 "password": "secreto123", "rol": "TIPSTER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(id.toString()))
                .andExpect(jsonPath("$.token").value("jwt.token"))
                .andExpect(jsonPath("$.rol").value("TIPSTER"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
    }

    @Test
    void debe_login_devolver_200_con_token() throws Exception {
        UUID id = UUID.randomUUID();
        when(autenticarUsuarioUseCase.ejecutar(any(AutenticarUsuarioComando.class)))
                .thenReturn(new AutenticacionResultado(id, "Ana", "ana@example.com", Rol.CLIENTE, "jwt.token",
                        LocalDateTime.of(2026, 8, 16, 10, 30)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email": "ana@example.com", "password": "secreto123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void debe_devolver_400_cuando_falta_password() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email": "ana@example.com"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debe_devolver_400_cuando_email_invalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType("application/json")
                        .content("""
                                {"nombre": "Ana", "email": "correo-mal",
                                 "password": "secreto123", "rol": "TIPSTER"}"""))
                .andExpect(status().isBadRequest());
    }
}
