-- V5__tarea_log_manual_nullable.sql
-- FASE T3 (poblamiento asíncrono): las ejecuciones MANUALES del poblamiento geográfico
-- registran TareaLog SIN tarea programada asociada (solo executionId).
--
-- [POR QUÉ] tarea_log.tarea_programada_id estaba NOT NULL: solo existían ejecuciones
--           de tareas programadas. Con la vía manual asíncrona ese campo es null.
-- [CUÁNDO] Ya aplicado a la BD dev (localhost:5434). Testcontainers no lo necesita
--          (el entity ahora declara la columna nullable y genera el DDL correcto).

ALTER TABLE tarea_log ALTER COLUMN tarea_programada_id DROP NOT NULL;
