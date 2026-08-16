// ─────────────────────────────────────────────
// [QUÉ]: Test de integración end-to-end de los nuevos endpoints GET: ligas, partidos,
//        cuotas y suscripciones. Valida consulta sobre datos reales en PostgreSQL
//        (Testcontainers) con autenticación JWT y roles.
// [POR QUÉ]: Los tests unitarios de controller no ejercitan la SecurityFilterChain,
//            ni el mapeo JPA real, ni la autorización por rol. Este test cierra esa
//            brecha validando el flujo completo: BD → adapter → repository → controller.
// [ALTERNATIVAS]: Solo tests unitarios + tests de adapter separados; se descartan
//                 porque no garantizan que los endpoints GET funcionen con seguridad.
// [RELACIONES]: LigaController, PartidoController, SuscripcionController,
//               SecurityConfig, JwtAuthenticationFilter, adapters JPA.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter.AbstractRepositoryJpaAdapterTest;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PartidoJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.SuscripcionJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ConsultasRestIntegrationTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LigaRepository ligaRepository;
    @Autowired
    private PaisRepository paisRepository;
    @Autowired
    private PartidoRepository partidoRepository;
    @Autowired
    private SuscripcionRepository suscripcionRepository;
    @Autowired
    private LigaJpaRepository ligaJpaRepository;
    @Autowired
    private PaisJpaRepository paisJpaRepository;
    @Autowired
    private PartidoJpaRepository partidoJpaRepository;
    @Autowired
    private SuscripcionJpaRepository suscripcionJpaRepository;
    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @BeforeEach
    void limpiar() {
        suscripcionJpaRepository.deleteAll();
        partidoJpaRepository.deleteAll();
        ligaJpaRepository.deleteAll();
        paisJpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    @Test
    void debe_listar_ligas_activas_con_token_tipster() throws Exception {
        UUID tipsterId = registrarUsuarioYExtraerId("tipster@example.com", "TIPSTER");
        String token = loginYExtraerToken("tipster@example.com", "clave-secreta");
        crearLigaActivaConPosiciones();

        mockMvc.perform(get("/api/v1/ligas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Premier League"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    void debe_obtener_detalle_de_liga_con_posiciones() throws Exception {
        registrarUsuario("admin@example.com", "SUPERADMIN");
        String token = loginYExtraerToken("admin@example.com", "clave-secreta");
        Liga liga = crearLigaActivaConPosiciones();

        mockMvc.perform(get("/api/v1/ligas/{id}", liga.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posiciones[0].equipoNombre").value("Arsenal"))
                .andExpect(jsonPath("$.posiciones[0].puntos").value(9));
    }

    @Test
    void debe_listar_partidos_por_liga() throws Exception {
        registrarUsuario("tipster@example.com", "TIPSTER");
        String token = loginYExtraerToken("tipster@example.com", "clave-secreta");
        Liga liga = crearLigaActivaConPosiciones();
        Partido partido = crearPartidoConCuotas(liga.id());

        mockMvc.perform(get("/api/v1/partidos")
                        .param("ligaId", liga.id().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoLocal").value("Arsenal"))
                .andExpect(jsonPath("$[0].equipoVisitante").value("Chelsea"));
    }

    @Test
    void debe_obtener_cuotas_de_partido() throws Exception {
        registrarUsuario("tipster@example.com", "TIPSTER");
        String token = loginYExtraerToken("tipster@example.com", "clave-secreta");
        Liga liga = crearLigaActivaConPosiciones();
        Partido partido = crearPartidoConCuotas(liga.id());

        mockMvc.perform(get("/api/v1/partidos/{id}/cuotas", partido.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mercado").value("UNO_X_DOS"))
                .andExpect(jsonPath("$[0].valor").value(1.85));
    }

    @Test
    void debe_listar_suscripciones_del_cliente_autenticado() throws Exception {
        UUID clienteId = registrarUsuarioYExtraerId("cliente@example.com", "CLIENTE");
        String token = loginYExtraerToken("cliente@example.com", "clave-secreta");
        crearSuscripcionActiva(clienteId);

        mockMvc.perform(get("/api/v1/suscripciones")
                        .param("clienteId", clienteId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planNombre").value("Pro"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    void debe_rechazar_suscripciones_de_otro_cliente() throws Exception {
        UUID clienteId = registrarUsuarioYExtraerId("cliente@example.com", "CLIENTE");
        String token = loginYExtraerToken("cliente@example.com", "clave-secreta");
        UUID otroClienteId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/suscripciones")
                        .param("clienteId", otroClienteId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: solo puedes consultar tus propias suscripciones"));
    }

    @Test
    void debe_listar_paises_del_catalogo_con_token_tipster() throws Exception {
        registrarUsuario("tipster@example.com", "TIPSTER");
        String token = loginYExtraerToken("tipster@example.com", "clave-secreta");
        paisRepository.guardar(new Pais("Colombia", "CO", "Sudamérica", "COL", "/teams/colombia/", false));
        paisRepository.guardar(new Pais("España", "ES", "Europa", "ESP", "/teams/espana/", true));

        mockMvc.perform(get("/api/v1/paises")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Colombia"))
                .andExpect(jsonPath("$[1].nombre").value("España"))
                .andExpect(jsonPath("$[1].isoAlpha2").value("ES"))
                .andExpect(jsonPath("$[1].continente").value("Europa"))
                .andExpect(jsonPath("$[1].mapeado").value(true));
    }

    @Test
    void debe_listar_ligas_borrador_del_catalogo_con_token_tipster() throws Exception {
        registrarUsuario("tipster@example.com", "TIPSTER");
        String token = loginYExtraerToken("tipster@example.com", "clave-secreta");
        Liga ligaBorrador = new Liga("LaLiga EA Sports", "España", new Temporada(2026, 2027),
                "/path/to/scrape/calendar", "api-football-140");
        ligaRepository.guardar(ligaBorrador);

        mockMvc.perform(get("/api/v1/ligas")
                        .param("estado", "BORRADOR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("LaLiga EA Sports"))
                .andExpect(jsonPath("$[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$[0].urlSoccerway").value("/path/to/scrape/calendar"))
                .andExpect(jsonPath("$[0].apiId").value("api-football-140"));
    }

    @Test
    void debe_rechazar_catalogo_paises_para_cliente() throws Exception {
        registrarUsuario("cliente@example.com", "CLIENTE");
        String token = loginYExtraerToken("cliente@example.com", "clave-secreta");

        mockMvc.perform(get("/api/v1/paises")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private Liga crearLigaActivaConPosiciones() {
        Liga liga = new Liga("Premier League", "Inglaterra", new Temporada(2024, 2025));
        liga.activar(true, true, true);
        Equipo arsenal = new Equipo(UUID.randomUUID(), "Arsenal");
        Equipo chelsea = new Equipo(UUID.randomUUID(), "Chelsea");
        liga.agregarEquipo(arsenal);
        liga.agregarEquipo(chelsea);
        PosicionTabla posicion = new PosicionTabla(arsenal, 1, 3, 3, 0, 0, 9, 2, 9, List.of());
        liga.actualizarPosiciones(List.of(posicion));
        ligaRepository.guardar(liga);
        return liga;
    }

    private Partido crearPartidoConCuotas(UUID ligaId) {
        Equipo arsenal = new Equipo(UUID.randomUUID(), "Arsenal");
        Equipo chelsea = new Equipo(UUID.randomUUID(), "Chelsea");
        Partido partido = new Partido(ligaId, arsenal, chelsea,
                new FechaProgramada(LocalDateTime.now().plusDays(1)));
        partido.actualizarCuotas(List.of(new Cuota(Mercado.UNO_X_DOS, new BigDecimal("1.85"))));
        partidoRepository.guardar(partido);
        return partido;
    }

    private void crearSuscripcionActiva(UUID clienteId) {
        UUID tipsterId = UUID.randomUUID();
        Suscripcion suscripcion = new Suscripcion(clienteId, tipsterId,
                new Plan("Pro", new BigDecimal("9.99"), 30), LocalDateTime.now());
        suscripcionRepository.guardar(suscripcion);
    }

    private void registrarUsuario(String email, String rol) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Usuario", "email": "%s",
                                 "password": "clave-secreta", "rol": "%s"}""".formatted(email, rol)))
                .andExpect(status().isCreated());
    }

    private UUID registrarUsuarioYExtraerId(String email, String rol) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Usuario", "email": "%s",
                                 "password": "clave-secreta", "rol": "%s"}""".formatted(email, rol)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("usuarioId").asText());
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
