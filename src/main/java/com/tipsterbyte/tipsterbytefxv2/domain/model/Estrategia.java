// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa una estrategia de pronóstico con criterios dinámicos.
// [POR QUÉ]: HU-16 — cada estrategia define una receta de criterios que se evalúan
//            contra partidos programados para generar pronósticos sugeridos con score.
//            Un tipster puede tener múltiples estrategias; solo las activas se evalúan.
// [ALTERNATIVAS]: Estrategia como VO compuesto; se descarta porque necesita identidad
//                 y lifecycle propio (CRUD, activar/desactivar).
// [RELACIONES]: CU-23 (gestión), CU-24 (evaluación), CU-25 (sugerencias).
//               Compone lista de `Criterio` (embeddable).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Estrategia {

    private final UUID id;
    private final String nombre;
    private final UUID tipsterId;
    private final Mercado mercado;
    private final Integer maxPartidos;
    private final BigDecimal confianzaMinima;
    private boolean activa;
    private final List<Criterio> criterios;
    private final List<UUID> ligaIds;
    private final Instant createdAt;

    public Estrategia(String nombre, UUID tipsterId, Mercado mercado,
                      Integer maxPartidos, BigDecimal confianzaMinima,
                      List<Criterio> criterios, List<UUID> ligaIds) {
        this(UUID.randomUUID(), nombre, tipsterId, mercado, maxPartidos, confianzaMinima,
                true, criterios, ligaIds, Instant.now());
    }

    public Estrategia(UUID id, String nombre, UUID tipsterId, Mercado mercado,
                      Integer maxPartidos, BigDecimal confianzaMinima, boolean activa,
                      List<Criterio> criterios, List<UUID> ligaIds, Instant createdAt) {
        if (id == null) throw new DomainException("Estrategia requiere id");
        if (nombre == null || nombre.isBlank()) throw new DomainException("Estrategia requiere nombre");
        if (tipsterId == null) throw new DomainException("Estrategia requiere tipsterId");
        if (mercado == null) throw new DomainException("Estrategia requiere mercado");
        if (maxPartidos != null && maxPartidos < 1) throw new DomainException("maxPartidos debe ser >= 1");
        if (confianzaMinima != null && (confianzaMinima.compareTo(BigDecimal.ZERO) < 0 || confianzaMinima.compareTo(BigDecimal.ONE) > 0)) {
            throw new DomainException("confianzaMinima debe estar entre 0 y 1");
        }
        this.id = id;
        this.nombre = nombre;
        this.tipsterId = tipsterId;
        this.mercado = mercado;
        this.maxPartidos = maxPartidos;
        this.confianzaMinima = confianzaMinima;
        this.activa = activa;
        this.criterios = new ArrayList<>(criterios != null ? criterios : List.of());
        this.ligaIds = new ArrayList<>(ligaIds != null ? ligaIds : List.of());
        this.createdAt = createdAt;
    }

    public void activar() { this.activa = true; }
    public void desactivar() { this.activa = false; }

    public void actualizarCriterios(List<Criterio> nuevosCriterios) {
        if (nuevosCriterios == null || nuevosCriterios.isEmpty()) {
            throw new DomainException("Estrategia debe tener al menos un criterio");
        }
        this.criterios.clear();
        this.criterios.addAll(nuevosCriterios);
    }

    public UUID id() { return id; }
    public String nombre() { return nombre; }
    public UUID tipsterId() { return tipsterId; }
    public Mercado mercado() { return mercado; }
    public Integer maxPartidos() { return maxPartidos; }
    public BigDecimal confianzaMinima() { return confianzaMinima; }
    public boolean activa() { return activa; }
    public List<Criterio> criterios() { return Collections.unmodifiableList(criterios); }
    public List<UUID> ligaIds() { return Collections.unmodifiableList(ligaIds); }
    public Instant createdAt() { return createdAt; }
}
