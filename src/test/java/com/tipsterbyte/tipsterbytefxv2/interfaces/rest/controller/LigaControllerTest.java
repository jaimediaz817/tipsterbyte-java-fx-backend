// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de LigaController (MockMvc standalone): cubre CU-04, CU-01..03
//        y los nuevos endpoints GET de consulta (listado, detalle, posiciones).
// [POR QUÉ]: Valida el contrato HTTP sin levantar el contexto Spring: mapeo de DTOs,
//            códigos de estado, y manejo de DomainException → 422.
// [RELACIONES]: LigaController → ActivarLigaUseCase, SincronizarXxxUseCase, LigaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.JornadaActualDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ObtenerJornadaActualUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
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
class LigaControllerTest {

    @Mock
    private ActivarLigaUseCase activarLigaUseCase;
    @Mock
    private SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    @Mock
    private SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    @Mock
    private SincronizarCuotasUseCase sincronizarCuotasUseCase;
    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private ObtenerJornadaActualUseCase obtenerJornadaActualUseCase;

    @Mock
    private com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new LigaController(activarLigaUseCase, sincronizarPosicionesUseCase,
                                sincronizarCalendarioUseCase, sincronizarCuotasUseCase, ligaRepository,
                                obtenerJornadaActualUseCase, sincronizarEquiposLigaUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_activar_liga_con_urls_y_devolver_204() throws Exception {
        UUID ligaId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ligas/{id}/activacion", ligaId)
                        .contentType("application/json")
                        .content("""
                                {"urlPosiciones": "https://flashscore.com/tabla",
                                 "urlCalendario": "https://soccerway.com/calendario",
                                 "urlCuotas": "https://wplay.co/ligas"}"""))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ActivarLigaComando> captor = ArgumentCaptor.forClass(ActivarLigaComando.class);
        verify(activarLigaUseCase).ejecutar(eq(ligaId), captor.capture());
        assertEquals("https://flashscore.com/tabla", captor.getValue().urlPosiciones());
        assertEquals("https://soccerway.com/calendario", captor.getValue().urlCalendario());
        assertEquals("https://wplay.co/ligas", captor.getValue().urlCuotas());
    }

    @Test
    void debe_devolver_422_cuando_la_liga_no_existe() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(activarLigaUseCase.ejecutar(eq(ligaId), any(ActivarLigaComando.class)))
                .thenThrow(new DomainException("Liga no encontrada: " + ligaId));

        mockMvc.perform(post("/api/v1/ligas/{id}/activacion", ligaId)
                        .contentType("application/json")
                        .content("""
                                {"urlPosiciones": "https://flashscore.com/tabla"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Liga no encontrada: " + ligaId));
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

    @Test
    void debe_listar_ligas_activas() throws Exception {
        Liga liga = unaLigaActiva();
        when(ligaRepository.buscarActivas()).thenReturn(List.of(liga));

        mockMvc.perform(get("/api/v1/ligas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(liga.id().toString()))
                .andExpect(jsonPath("$[0].nombre").value("Premier League"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    void debe_listar_ligas_borrador_con_urls_de_fuente() throws Exception {
        Liga liga = unaLigaBorradorCatalogo();
        when(ligaRepository.buscarPorEstado(EstadoLiga.BORRADOR)).thenReturn(List.of(liga));

        mockMvc.perform(get("/api/v1/ligas").param("estado", "BORRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("LaLiga EA Sports"))
                .andExpect(jsonPath("$[0].pais").value("España"))
                .andExpect(jsonPath("$[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$[0].temporada").value("2026/2027"))
                .andExpect(jsonPath("$[0].urlSoccerway").value("/path/to/scrape/calendar"))
                .andExpect(jsonPath("$[0].apiId").value("api-football-140"));
    }

    @Test
    void debe_filtrar_ligas_por_pais() throws Exception {
        Liga liga = unaLigaBorradorCatalogo();
        when(ligaRepository.buscarPorEstadoYPais(EstadoLiga.BORRADOR, "España"))
                .thenReturn(List.of(liga));

        mockMvc.perform(get("/api/v1/ligas").param("estado", "BORRADOR").param("pais", "España"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("LaLiga EA Sports"))
                .andExpect(jsonPath("$[0].pais").value("España"));
    }

    @Test
    void debe_filtrar_ligas_por_pais_sin_estado_dentro_del_scope_activa() throws Exception {
        UUID ligaId = UUID.randomUUID();
        Liga liga = Liga.reconstruir(
                ligaId, "LaLiga EA Sports", "España", null,
                EstadoLiga.ACTIVA, temporadasDe(ligaId, "2026/2027", 2026, 2027));
        when(ligaRepository.buscarPorEstadoYPais(EstadoLiga.ACTIVA, "España"))
                .thenReturn(List.of(liga));

        mockMvc.perform(get("/api/v1/ligas").param("pais", "España"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("LaLiga EA Sports"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));

        verify(ligaRepository).buscarPorEstadoYPais(EstadoLiga.ACTIVA, "España");
    }

    @Test
    void debe_devolver_400_cuando_estado_es_invalido() throws Exception {
        mockMvc.perform(get("/api/v1/ligas").param("estado", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void debe_obtener_detalle_de_liga_con_posiciones() throws Exception {
        Liga liga = unaLigaConPosiciones();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        mockMvc.perform(get("/api/v1/ligas/{id}", liga.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(liga.id().toString()))
                .andExpect(jsonPath("$.posiciones[0].equipoNombre").value("Arsenal"))
                .andExpect(jsonPath("$.posiciones[0].posicion").value(1));
    }

    @Test
    void debe_devolver_422_cuando_liga_de_detalle_no_existe() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ligas/{id}", ligaId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Liga no encontrada: " + ligaId));
    }

    @Test
    void debe_obtener_posiciones_de_liga() throws Exception {
        Liga liga = unaLigaConPosiciones();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        mockMvc.perform(get("/api/v1/ligas/{id}/posiciones", liga.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipoNombre").value("Arsenal"))
                .andExpect(jsonPath("$[0].puntos").value(9));
    }

    @Test
    void debe_obtener_jornada_actual_de_liga() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(obtenerJornadaActualUseCase.ejecutar(ligaId)).thenReturn(new JornadaActualDto(12, 13));

        mockMvc.perform(get("/api/v1/ligas/{id}/jornada-actual", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jornadaActual").value(12))
                .andExpect(jsonPath("$.proximaJornada").value(13));
    }

    @Test
    void debe_devolver_jornada_nula_cuando_no_hay_partidos_con_jornada() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(obtenerJornadaActualUseCase.ejecutar(ligaId)).thenReturn(new JornadaActualDto(null, null));

        mockMvc.perform(get("/api/v1/ligas/{id}/jornada-actual", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jornadaActual").value(nullValue()))
                .andExpect(jsonPath("$.proximaJornada").value(nullValue()));
    }

    @Test
    void debe_devolver_plantilla_de_equipos_de_la_temporada_vigente_con_escudos() throws Exception {
        Liga liga = unaLigaConEquiposYEscudos();
        when(ligaRepository.buscarPorId(liga.id())).thenReturn(Optional.of(liga));

        mockMvc.perform(get("/api/v1/ligas/{ligaId}/equipos", liga.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ligaId").value(liga.id().toString()))
                .andExpect(jsonPath("$.temporadaNombre").value("2024/2025"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.equipos[0].nombre").value("Arsenal"))
                .andExpect(jsonPath("$.equipos[0].logoUrl").value("https://escudos/arsenal.png"))
                .andExpect(jsonPath("$.equipos[1].nombre").value("Chelsea"));
    }

    @Test
    void debe_devolver_422_al_consultar_equipos_de_liga_inexistente() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ligas/{ligaId}/equipos", ligaId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void debe_sincronizar_equipos_de_liga_y_devolver_conteos() throws Exception {
        UUID ligaId = UUID.randomUUID();
        when(sincronizarEquiposLigaUseCase.ejecutar(ligaId))
                .thenReturn(new com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase.ResultadoPoblacionEquipos(5, 2, 30));

        mockMvc.perform(post("/api/v1/ligas/{ligaId}/equipos/sincronizar", ligaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados").value(5))
                .andExpect(jsonPath("$.actualizados").value(2))
                .andExpect(jsonPath("$.totalEquipos").value(30));
    }

    private static Set<Temporada> temporadasDe(UUID ligaId, String nombre, int inicio, int fin) {
        return Set.of(new Temporada(ligaId, nombre, null, inicio, fin,
                EstadoTemporada.PLANIFICADA));
    }

    private Liga unaLigaConEquiposYEscudos() {
        UUID ligaId = UUID.randomUUID();
        Equipo arsenal = new Equipo(UUID.randomUUID(), "Arsenal", "https://escudos/arsenal.png");
        Equipo chelsea = new Equipo(UUID.randomUUID(), "Chelsea", null);
        Set<Temporada> temporadas = Set.of(new Temporada(
                UUID.randomUUID(), ligaId, "2024/2025", null, 2024, 2025,
                EstadoTemporada.PLANIFICADA,
                List.of(arsenal, chelsea), List.of()));
        return Liga.reconstruir(
                ligaId, "Premier League", "Inglaterra", null,
                EstadoLiga.ACTIVA, temporadas);
    }

    private Liga unaLigaActiva() {
        UUID ligaId = UUID.randomUUID();
        return Liga.reconstruir(
                ligaId, "Premier League", "Inglaterra", null,
                EstadoLiga.ACTIVA, temporadasDe(ligaId, "2024/2025", 2024, 2025));
    }

    private Liga unaLigaBorradorCatalogo() {
        UUID ligaId = UUID.randomUUID();
        return Liga.reconstruir(
                ligaId, "LaLiga EA Sports", "España", null,
                EstadoLiga.BORRADOR, "/path/to/scrape/calendar", "api-football-140",
                temporadasDe(ligaId, "2026/2027", 2026, 2027));
    }

    private Liga unaLigaConPosiciones() {
        UUID ligaId = UUID.randomUUID();
        Equipo arsenal = new Equipo(UUID.randomUUID(), "Arsenal");
        PosicionTabla posicion = new PosicionTabla(arsenal, 1, 3, 3, 0, 0, 9, 2, 9, List.of());
        Set<Temporada> temporadas = Set.of(new Temporada(
                UUID.randomUUID(), ligaId, "2024/2025", null, 2024, 2025,
                EstadoTemporada.PLANIFICADA,
                List.of(arsenal), List.of(posicion)));
        return Liga.reconstruir(
                ligaId, "Premier League", "Inglaterra", null,
                EstadoLiga.ACTIVA, temporadas);
    }
}
