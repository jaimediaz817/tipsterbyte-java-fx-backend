// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ObtenerJornadaActualUseCase (CU-02): calcula la jornada
//        actual de una liga a partir del calendario persistido (próximo partido por
//        jugarse, o la última jornada si todo ya se jugó).
// [POR QUÉ]: Valida la regla de derivación (orden por fecha, filtro >= ahora, fallback
//            a la última jornada) sin levantar Spring (regla testing.md: use cases
//            con Mockito).
// [RELACIONES]: CU-02 → PartidoRepository → LigaController (/jornada-actual).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.JornadaActualDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerJornadaActualUseCaseTest {

    @Mock
    private PartidoRepository partidoRepository;

    private ObtenerJornadaActualUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new ObtenerJornadaActualUseCase(partidoRepository);
    }

    @Test
    void debe_devolver_jornadas_nulas_sin_partidos_con_jornada() {
        UUID ligaId = UUID.randomUUID();
        when(partidoRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                unPartido(ligaId, LocalDateTime.now().plusDays(1), null)));

        JornadaActualDto dto = casoDeUso.ejecutar(ligaId);

        assertNull(dto.jornadaActual());
        assertNull(dto.proximaJornada());
    }

    @Test
    void debe_tomar_la_jornada_del_proximo_partido_por_jugarse() {
        UUID ligaId = UUID.randomUUID();
        when(partidoRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                unPartido(ligaId, LocalDateTime.now().plusDays(15), 5),
                unPartido(ligaId, LocalDateTime.now().minusDays(2), 3),
                unPartido(ligaId, LocalDateTime.now().plusDays(3), 4)));

        JornadaActualDto dto = casoDeUso.ejecutar(ligaId);

        assertEquals(4, dto.jornadaActual());
        assertEquals(5, dto.proximaJornada());
    }

    @Test
    void debe_tomar_la_ultima_jornada_cuando_todo_ya_se_jugo() {
        UUID ligaId = UUID.randomUUID();
        when(partidoRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                unPartido(ligaId, LocalDateTime.now().minusDays(9), 4),
                unPartido(ligaId, LocalDateTime.now().minusDays(1), 5)));

        JornadaActualDto dto = casoDeUso.ejecutar(ligaId);

        assertEquals(5, dto.jornadaActual());
        assertEquals(6, dto.proximaJornada());
    }

    @Test
    void debe_ordenar_por_fecha_aunque_el_repositorio_no_ordene() {
        UUID ligaId = UUID.randomUUID();
        when(partidoRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                unPartido(ligaId, LocalDateTime.now().plusDays(10), 6),
                unPartido(ligaId, LocalDateTime.now().plusDays(1), 4),
                unPartido(ligaId, LocalDateTime.now().plusDays(5), 5)));

        JornadaActualDto dto = casoDeUso.ejecutar(ligaId);

        assertEquals(4, dto.jornadaActual());
        assertEquals(5, dto.proximaJornada());
    }

    private Partido unPartido(UUID ligaId, LocalDateTime fecha, Integer jornada) {
        return new Partido(ligaId, new Equipo("Local"), new Equipo("Visitante"),
                new FechaProgramada(fecha), jornada);
    }
}