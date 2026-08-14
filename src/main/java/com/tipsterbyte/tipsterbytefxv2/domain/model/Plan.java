// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa un plan de suscripción (nombre, precio y
//        duración en días).
// [POR QUÉ]: El plan es un concepto cerrado con reglas: precio no negativo y
//            duración positiva. Aísla esas validaciones en un solo lugar.
// [ALTERNATIVAS]: Campos sueltos en Suscripcion; se descarta porque el plan es
//                 un objeto de valor reutilizable y con invariantes propias.
// [RELACIONES]: Usado por el aggregate Suscripcion (CU-09).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.math.BigDecimal;

public record Plan(String nombre, BigDecimal precio, int duracionDias) {

    // [QUÉ]: Compact constructor que valida precio no negativo y duración positiva.
    public Plan {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Plan requiere nombre");
        }
        if (precio == null || precio.signum() < 0) {
            throw new DomainException("Plan inválido: precio no puede ser negativo");
        }
        if (duracionDias <= 0) {
            throw new DomainException("Plan inválido: duración debe ser mayor a cero");
        }
    }
}