// ─────────────────────────────────────────────
// [QUÉ]: Test de integración end-to-end del catálogo (CU-10): POST /api/v1/catalogo/activar
//        dispara la sincronización real (use case + adapters JPA + PostgreSQL) con los
//        proveedores #1/#5 mockeados, y GET /api/v1/catalogo/estado devuelve el estado
//        derivado (VACIO/POBLADO) con los conteos.
// [POR QUÉ]: Cierra la cadena completa panel SUPERADMIN: SecurityFilterChain (rol) →
//            CatalogoController → use cases → adapters JPA → PostgreSQL, validando que
//            la activación persiste de verdad y que el estado refleja los datos.
// [ALTERNATIVAS]: Levantar el servicio Python real; se descarta porque añade una
//                 dependencia externa frágil a la suite (misma decisión que en CU-10).
// [RELACIONES]: CU-10 → CatalogoController + SincronizarCatalogoUseCase +
//               ConsultarEstadoCatalogoUseCase + SecurityConfig.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter.AbstractRepositoryJpaAdapterTest;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CatalogoControllerIntegrationTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaisJpaRepository paisJpaRepository;
    @Autowired
    private LigaJpaRepository ligaJpaRepository;
    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @MockitoBean
    private ProveedorPaises proveedorPaises;

    @MockitoBean
    private ProveedorLigasPorPais proveedorLigasPorPais;

    @BeforeEach
    void limpiar() {
        ligaJpaRepository.deleteAll();
        paisJpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    @Test
    void debe_activar_catalogo_y_devolver_estado_poblado() throws Exception {
        registrarUsuario("admin@example.com", "SUPERADMIN");
        String token = loginYExtraerToken("admin@example.com", "clave-secreta");
        simularFuentesConDatos();

        mockMvc.perform(post("/api/v1/catalogo/activar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("POBLADO"))
                .andExpect(jsonPath("$.totalPaises").value(1))
                .andExpect(jsonPath("$.totalLigas").value(1));
    }

    @Test
    void debe_devolver_estado_vacio_antes_de_activar() throws Exception {
        registrarUsuario("admin@example.com", "SUPERADMIN");
        String token = loginYExtraerToken("admin@example.com", "clave-secreta");

        mockMvc.perform(get("/api/v1/catalogo/estado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VACIO"))
                .andExpect(jsonPath("$.totalPaises").value(0))
                .andExpect(jsonPath("$.totalLigas").value(0));
    }

    @Test
    void debe_rechazar_activacion_para_cliente() throws Exception {
        registrarUsuario("cliente@example.com", "CLIENTE");
        String token = loginYExtraerToken("cliente@example.com", "clave-secreta");

        mockMvc.perform(post("/api/v1/catalogo/activar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private void simularFuentesConDatos() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                new PaisFuente("España", "/espana/", "81", "ES", "Europa", true)));
        when(proveedorLigasPorPais.obtenerLigasPorPais("España", 0)).thenReturn(List.of(
                new LigaFuente("LaLiga EA Sports", "League", "", null,
                        "https://co.soccerway.com/espana/laliga-ea-sports/", "2026/2027")));
    }

    private void registrarUsuario(String email, String rol) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Usuario", "email": "%s",
                                 "password": "clave-secreta", "rol": "%s"}""".formatted(email, rol)))
                .andExpect(status().isCreated());
    }

    private String loginYExtraerToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}