---
name: ddd-domain-modeling
description: Use when modeling or extending domain concepts: entities, value objects, aggregates, domain events and business rules. Grounded in docs/domain/modelo-dominio.md (BR-001..008).
---

# DDD Domain Modeling — tipsterbyte-fx-v2

## Cuándo usar

Al diseñar o modificar el modelo de dominio: nuevas entidades, VOs, aggregates, eventos o reglas de negocio. Fuente: `docs/domain/modelo-dominio.md`.

## Conceptos y su aplicación aquí

| Concepto | En este proyecto |
| --- | --- |
| Aggregate Root | `Liga`, `Partido`, `Pronostico`, `Suscripcion` |
| Entity | `Equipo`, `Tipster`, `Cliente` |
| Value Object | `Cuota`, `Resultado`, `Temporada`, `PosicionTabla`, `Mercado`, `SeleccionPronostico`, `Email`, `Plan` |
| Domain Event | `LigaActivada`, `PartidoProgramado`, `CuotaActualizada`, `PronosticoPublicado`, `SuscripcionCreada` |

## Decisiones de modelado

- **Entity vs VO**: tiene identidad propia que permanece → Entity; se define por sus valores e importa su igualdad → VO.
- **Aggregate boundary**: la consistencia transaccional dentro del aggregate se protege desde el root; no se accede a miembros internos sin pasar por él.
- **Domain Service**: solo cuando una regla no pertenece naturalmente a una entidad/VO (ej: `CalculadoraPosiciones`).
- **Events**: se emiten cuando ocurre un hecho relevante para otros bounded contexts (notificaciones, cache, ingesta). No son para cualquier cambio.

## Reglas de negocio (BR-001..008)

Violaciones lanzan `DomainException` con el BR referenciado. Ejemplo:

```java
if (!fuentesOperativas) throw new DomainException("Liga no activable: fuentes no operativas (BR-001)");
```

## Proceso para modelar algo nuevo

1. ¿Es Entity o VO? ¿Dónde vive (aggregate)?
2. ¿Qué reglas de negocio lo gobiernan? ¿Nueva BR?
3. ¿Emitiría un evento relevante? ¿Para quién?
4. ¿Qué casos de uso (CU-xx) lo tocan? Registrar trazabilidad.