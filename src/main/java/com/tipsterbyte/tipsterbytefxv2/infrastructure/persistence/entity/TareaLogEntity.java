package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Entidad JPA que representa un log de ejecución de tarea programada.
 * [POR QUÉ]: Mapea el dominio TareaLog a la tabla tarea_log para persistencia.
 * [ALTERNATIVAS]: No usar JPA y escribir SQL manual; se descarta porque
 *                 Spring Data JPA reduce el boilerplate.
 * [RELACIONES]: TareaLogRepositoryJpaAdapter convierte entre esta entidad y
 *                el dominio TareaLog.
 * ─────────────────────────────────────────────
 */
@Entity
@Table(name = "tarea_log")
public class TareaLogEntity {

    @Id
    private UUID id;

    // [POR QUÉ]: Nullable desde FASE T3 — las ejecuciones MANUALES del poblamiento
    //            geográfico no tienen tarea programada asociada (solo executionId).
    @Column(name = "tarea_programada_id")
    private UUID tareaProgramadaId;

    @Column(name = "execution_id", nullable = false, length = 36)
    private String executionId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "mensaje")
    private String mensaje;

    // [QUÉ]: HU-14 AC7 — cantidad explícita de elementos procesados en la corrida.
    //        Permite al frontend mostrar "sin datos aún" (elementosProcesados=0)
    //        sin parsear el texto del mensaje.
    @Column(name = "elementos_procesados")
    private Integer elementosProcesados;

    // Constructors
    public TareaLogEntity() {
    }

    public TareaLogEntity(UUID id, UUID tareaProgramadaId, String executionId,
                          Instant timestamp, String status, Long durationMs,
                          String errorCode, String mensaje, Integer elementosProcesados) {
        this.id = id;
        this.tareaProgramadaId = tareaProgramadaId;
        this.executionId = executionId;
        this.timestamp = timestamp;
        this.status = status;
        this.durationMs = durationMs;
        this.errorCode = errorCode;
        this.mensaje = mensaje;
        this.elementosProcesados = elementosProcesados;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTareaProgramadaId() {
        return tareaProgramadaId;
    }

    public void setTareaProgramadaId(UUID tareaProgramadaId) {
        this.tareaProgramadaId = tareaProgramadaId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Integer getElementosProcesados() {
        return elementosProcesados;
    }

    public void setElementosProcesados(Integer elementosProcesados) {
        this.elementosProcesados = elementosProcesados;
    }

    /**
     * Convierte esta entidad a su representación de dominio.
     */
    public TareaLog toDomainModel() {
        return new TareaLog(
                id,
                tareaProgramadaId,
                executionId,
                timestamp,
                status,
                durationMs,
                errorCode,
                mensaje,
                elementosProcesados
        );
    }

    /**
     * Convierte un dominio TareaLog a esta entidad.
     */
    public static TareaLogEntity fromDomainModel(TareaLog log) {
        return new TareaLogEntity(
                log.id(),
                log.tareaProgramadaId(),
                log.executionId(),
                log.timestamp(),
                log.status(),
                log.durationMs(),
                log.errorCode(),
                log.mensaje(),
                log.elementosProcesados()
        );
    }
}
