---
name: java-engineering
description: Use when writing, reviewing or refactoring Java code in this repository. Covers Java core best practices, immutability, exceptions, Optional, and value object design aligned with the project's domain.
---

# Java Engineering — tipsterbyte-fx-v2

## Cuándo usar

Cada vez que se escribe, revisa o refactoriza código Java. Consulta también `.opencode/rules/java.md` (regla siempre activa).

## Principios

- **Immutabilidad**: Value Objects inmutables con campos `final`, constructor con validación y sin setters.
- **Dominio puro**: las clases de `domain` NO llevan anotaciones de Spring/JPA ni imports de infraestructura.
- **Composición sobre herencia**: evitar jerarquías profundas; favorecer interfaces y composición.
- **Optional**: para ausencia de valores; nunca devolver `null` como contrato. No usar `Optional` como parámetro de método.
- **Excepciones de dominio**: `DomainException` para violaciones de reglas de negocio (BR-xx), separada de errores de infraestructura.
- **Sin logging en dominio**: solo application/infrastructure/interfaces.

## Value Object (patrón)

```java
public final class Cuota {
    private final BigDecimal valor;

    public Cuota(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ONE) <= 0) {
            throw new DomainException("Cuota debe ser mayor que 1.0 (BR-007)");
        }
        this.valor = valor;
    }

    public BigDecimal valor() { return valor; }
}
```

## Checklist por clase

- [ ] Cabecera `[QUÉ]/[POR QUÉ]/[ALTERNATIVAS]/[RELACIONES]` (rule `documentation.md`)
- [ ] Nombres del Ubiquitous Language (`docs/domain/modelo-dominio.md`)
- [ ] Inmutable si es VO; identidad si es Entity
- [ ] Test unitario (rule `testing.md`)