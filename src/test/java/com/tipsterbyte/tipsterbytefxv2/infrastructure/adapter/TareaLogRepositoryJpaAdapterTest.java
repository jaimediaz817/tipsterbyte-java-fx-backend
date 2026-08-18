// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de TareaLogRepositoryJpaAdapter contra PostgreSQL
//        (Testcontainers): guardar un log de ejecución y recuperar los logs de una
//        tarea programada ordenados por timestamp descendente.
// [POR QUÉ]: Cierra la cadena application → port → adapter JPA → PostgreSQL para la
//            trazabilidad de ejecuciones del scheduler (regla testing.md: adapters
//            con Testcontainers).
// [RELACIONES]: FASE 12.6 → TareaLogRepositoryJpaAdapter + TareaLog (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TareaLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TareaLogRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private TareaLogRepository tareaLogRepository;

    @Autowired
    private TareaLogJpaRepository tareaLogJpaRepository;

    @BeforeEach
    void limpiar() {
        tareaLogJpaRepository.deleteAll();
    }

    @Test
    void debe_guardar_y_recuperar_logs_de_una_tarea_ordenados_descendente() {
        UUID tareaId = UUID.randomUUID();
        tareaLogRepository.guardar(new TareaLog(UUID.randomUUID(), tareaId, "exec-1",
                Instant.parse("2026-01-01T10:00:00Z"), "SUCCESS", 150L, null, "ok"));
        tareaLogRepository.guardar(new TareaLog(UUID.randomUUID(), tareaId, "exec-2",
                Instant.parse("2026-01-01T11:00:00Z"), "ERROR", 90L, "RuntimeException", "fallo"));

        List<TareaLog> logs = tareaLogRepository.buscarPorTareaProgramadaId(tareaId);

        assertEquals(2, logs.size());
        // El más reciente primero (timestamp descendente).
        assertEquals("exec-2", logs.get(0).executionId());
        assertEquals("ERROR", logs.get(0).status());
        assertEquals("RuntimeException", logs.get(0).errorCode());
        assertEquals("exec-1", logs.get(1).executionId());
    }

    @Test
    void debe_retornar_vacio_cuando_no_hay_logs_para_la_tarea() {
        List<TareaLog> logs = tareaLogRepository.buscarPorTareaProgramadaId(UUID.randomUUID());

        assertTrue(logs.isEmpty());
    }

    @Test
    void debe_listar_las_ultimas_ejecuciones_globales_limitadas() {
        tareaLogRepository.guardar(new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-1",
                Instant.parse("2026-01-01T10:00:00Z"), "SUCCESS", 150L, null, "ok"));
        tareaLogRepository.guardar(new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-2",
                Instant.parse("2026-01-01T11:00:00Z"), "ERROR", 90L, "RuntimeException", "fallo"));
        tareaLogRepository.guardar(new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-3",
                Instant.parse("2026-01-01T12:00:00Z"), "SUCCESS", 200L, null, "ok"));

        List<TareaLog> ultimas = tareaLogRepository.listarUltimas(2);

        assertEquals(2, ultimas.size());
        // El más reciente primero y el límite respetado.
        assertEquals("exec-3", ultimas.get(0).executionId());
        assertEquals("exec-2", ultimas.get(1).executionId());
    }
}