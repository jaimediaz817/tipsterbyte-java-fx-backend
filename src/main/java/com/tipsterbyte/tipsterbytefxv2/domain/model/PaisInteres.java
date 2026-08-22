// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un país de interés para el poblamiento del catálogo
//        (fuente #1): un país preferido con su orden de prioridad (1 = primero) y
//        límite opcional de ligas a sincronizar por país.
// [POR QUÉ]: CU-10 ordena la sincronización con los países de interés primero y el
//            resto después (sin omitir ninguno). Es una lista curada por el usuario
//            con orden, distinta del catálogo de países persistidos (Pais): puede
//            registrarse antes de que el país exista en paises, por eso se referencia
//            por iso_alpha2 (clave natural de la fuente #1) y no por FK.
//            El campo maxLigasPorPais permite limitar cuántas ligas se extraen por
//            país en la sincronización (null = sin límite).
// [ALTERNATIVAS]: Flag/columna en Pais; se descarta porque la prioridad es una
//                 propiedad de lista (orden), no un atributo del país, y mezclaría
//                 preferencia con catálogo (ruido de nulls para países no preferidos).
// [RELACIONES]: CU-14 → PaisInteresRepository; consumido por CU-10 (SincronizarCatalogoUseCase).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class PaisInteres {

    private final UUID id;
    private final String isoAlpha2;
    private final String nombre;
    private final int prioridad;
    private final Integer maxLigasPorPais;

    // [QUÉ]: Construye un país de interés generando su identidad (alta vía CU-14).
    public PaisInteres(String isoAlpha2, String nombre, int prioridad, Integer maxLigasPorPais) {
        if (isoAlpha2 == null || isoAlpha2.isBlank()) {
            throw new DomainException("PaisInteres requiere isoAlpha2");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("PaisInteres requiere nombre");
        }
        if (prioridad <= 0) {
            throw new DomainException("PaisInteres requiere prioridad positiva");
        }
        this.id = UUID.randomUUID();
        this.isoAlpha2 = isoAlpha2.trim().toUpperCase();
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.maxLigasPorPais = maxLigasPorPais;
    }

    // [QUÉ]: Construye un país de interés con identidad provista (reconstrucción).
    public PaisInteres(UUID id, String isoAlpha2, String nombre, int prioridad, Integer maxLigasPorPais) {
        if (id == null) {
            throw new DomainException("PaisInteres requiere id");
        }
        if (isoAlpha2 == null || isoAlpha2.isBlank()) {
            throw new DomainException("PaisInteres requiere isoAlpha2");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("PaisInteres requiere nombre");
        }
        if (prioridad <= 0) {
            throw new DomainException("PaisInteres requiere prioridad positiva");
        }
        this.id = id;
        this.isoAlpha2 = isoAlpha2.trim().toUpperCase();
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.maxLigasPorPais = maxLigasPorPais;
    }

    public UUID id() {
        return id;
    }

    public String isoAlpha2() {
        return isoAlpha2;
    }

    public String nombre() {
        return nombre;
    }

    public int prioridad() {
        return prioridad;
    }

    public Integer maxLigasPorPais() {
        return maxLigasPorPais;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaisInteres que)) return false;
        return id.equals(que.id) &&
                isoAlpha2.equals(que.isoAlpha2) &&
                nombre.equals(que.nombre) &&
                prioridad == que.prioridad &&
                Objects.equals(maxLigasPorPais, que.maxLigasPorPais);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isoAlpha2, nombre, prioridad, maxLigasPorPais);
    }
}