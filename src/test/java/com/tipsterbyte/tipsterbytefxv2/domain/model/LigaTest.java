// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del aggregate Liga con su colección de temporadas.
// [POR QUÉ]: Verifica las reglas BR-001 (activación solo con fuentes operativas) y
//            BR-002 (no extraer posiciones de liga inactiva), la gestión de temporadas
//            (add/remove/actual/porNombre, pertenencia a la liga) y la reconstrucción
//            desde persistencia sin emitir eventos.
// [RELACIONES]: Aggregate root de CU-01..CU-04; compone Temporada, Equipo, PosicionTabla.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LigaTest {

    private static final Equipo EQUIPO = new Equipo("Equipo A");

    private static Liga ligaConTemporada(String nombreTemporada, int inicio, int fin) {
        Liga liga = new Liga("La Liga", "España");
        liga.addTemporada(new Temporada(liga.id(), nombreTemporada, null, inicio, fin,
                EstadoTemporada.PLANIFICADA));
        return liga;
    }

    @Test
    void debe_activarse_cuando_fuentes_operativas_br001() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        liga.activar(true, true, true);
        assertEquals(EstadoLiga.ACTIVA, liga.estado());
    }

    @Test
    void debe_rechazar_activacion_sin_fuentes_operativas_br001() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        assertThrows(DomainException.class, () -> liga.activar(true, true, false));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
    }

    @Test
    void debe_emitir_evento_liga_activada_al_activar() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        liga.activar(true, true, true);
        List<DomainEvent> eventos = liga.pullEventos();
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("LigaActivada")));
    }

    @Test
    void debe_rechazar_extraccion_de_posiciones_en_liga_inactiva_br002() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        PosicionTabla posicion = new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10);
        assertThrows(DomainException.class, () -> liga.actualizarPosiciones(List.of(posicion)));
    }

    @Test
    void debe_actualizar_posiciones_en_liga_activa() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        liga.activar(true, true, true);
        PosicionTabla posicion = new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10);
        liga.actualizarPosiciones(List.of(posicion));
        assertEquals(1, liga.posiciones().size());
    }

    @Test
    void debe_rechazar_agregar_equipo_duplicado() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        liga.agregarEquipo(EQUIPO);
        assertThrows(DomainException.class, () -> liga.agregarEquipo(EQUIPO));
    }

    @Test
    void debe_agregar_temporada_de_su_propia_liga() {
        Liga liga = new Liga("La Liga", "España");
        liga.addTemporada(new Temporada(liga.id(), "2025/2026", null, 2025, 2026, null));
        assertEquals(1, liga.getTemporadas().size());
    }

    @Test
    void debe_rechazar_temporada_de_otra_liga() {
        Liga liga = new Liga("La Liga", "España");
        UUID otraLigaId = UUID.randomUUID();
        assertThrows(DomainException.class, () ->
                liga.addTemporada(new Temporada(otraLigaId, "2025/2026", null, 2025, 2026, null)));
    }

    @Test
    void debe_resolver_temporada_actual_y_por_nombre() {
        Liga liga = ligaConTemporada("Apertura", 2025, 2026);
        assertTrue(liga.getTemporadaActual().isEmpty(), "PLANIFICADA no es temporada actual");
        assertEquals("Apertura", liga.getTemporadaPorNombre("apertura").orElseThrow().nombre());

        liga.getTemporadas().forEach(t -> { });
        Temporada activa = new Temporada(liga.id(), "Clausura", 1, 2026, 2027, EstadoTemporada.ACTIVA);
        liga.addTemporada(activa);
        assertEquals("Clausura", liga.getTemporadaActual().orElseThrow().nombre());
    }

    @Test
    void debe_eliminar_temporada() {
        Liga liga = ligaConTemporada("2025/2026", 2025, 2026);
        Temporada temporada = liga.getTemporadas().iterator().next();
        liga.removeTemporada(temporada);
        assertTrue(liga.getTemporadas().isEmpty());
    }

    @Test
    void debe_reconstruir_con_temporadas_equipos_y_posiciones_sin_eventos() {
        UUID ligaId = UUID.randomUUID();
        Set<Temporada> temporadas = Set.of(new Temporada(
                UUID.randomUUID(), ligaId, "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA,
                List.of(EQUIPO),
                List.of(new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10))));
        Liga liga = Liga.reconstruir(
                ligaId, "La Liga", "España", null, EstadoLiga.ACTIVA, temporadas);
        assertEquals(1, liga.equipos().size());
        assertEquals(1, liga.posiciones().size());
        assertEquals(1, liga.getTemporadas().size());
        assertEquals(EstadoLiga.ACTIVA, liga.estado());
        assertTrue(liga.pullEventos().isEmpty(), "reconstruir no debe emitir eventos");
    }

    @Test
    void debe_delegar_equipos_y_posiciones_en_la_temporada_vigente() {
        UUID ligaId = UUID.randomUUID();
        Temporada temporada = new Temporada(
                UUID.randomUUID(), ligaId, "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA,
                List.of(EQUIPO),
                List.of(new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10)));
        Liga liga = Liga.reconstruir(
                ligaId, "La Liga", "España", null, EstadoLiga.ACTIVA, Set.of(temporada));

        // La vista de la liga refleja los datos de SU temporada (delegación).
        assertEquals(1, liga.equipos().size());
        assertEquals(1, liga.posiciones().size());
    }

    @Test
    void debe_reconstruir_con_url_api_id_y_pais_id() {
        UUID ligaId = UUID.randomUUID();
        UUID paisId = UUID.randomUUID();
        Liga liga = Liga.reconstruir(
                ligaId, "La Liga", "España", paisId, EstadoLiga.BORRADOR,
                "/espana/la-liga/", "api-1", Set.of());
        assertEquals("/espana/la-liga/", liga.urlSoccerway());
        assertEquals("api-1", liga.apiId());
        assertEquals(paisId, liga.paisId());
    }

    @Test
    void debe_reconstruir_rechazando_nulos() {
        assertThrows(DomainException.class, () ->
                Liga.reconstruir(null, "La Liga", "España", null, EstadoLiga.BORRADOR,
                        Set.of()));
    }
}
