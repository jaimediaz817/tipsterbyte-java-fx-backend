# Regla de Java — tipsterbyte-fx-v2

> Regla SIEMPRE obligatoria al escribir código Java.

## Versiones (se congelan en FASE 2)

- Java + Gradle + Spring Boot (ADR-003: estándar del proceso de selección + coherencia con proyecto previo). Versión exacta se decide y documenta en FASE 2 (ver `docs/PROYECTO-PLAN.md`).
- No agregar dependencias sin documentar el `[POR QUÉ]` y la alternativa descartada.

## Estilo de código

- POJOs puros en `domain` (sin anotaciones de Spring/JPA). La infraestructura mapea.
- Value Objects **inmutables** (final + constructor de validación). Sin setters.
- Entities con identidad; id generado por infraestructura (UUID o DB).
- Preferir composición sobre herencia; evitar `@Data` de Lombok para dominio con reglas (usar constructor + métodos de negocio).
- `Optional` para valores ausentes; nunca `null` como contrato de retorno.
- Manejar excepciones de dominio (`DomainException`) separadas de las de infraestructura.
- Sin logs en dominio; logging solo en application/infrastructure/interfaces.

## Dependencias (hoja de ruta)

No se agregan hoy. Llegan en sus fases: JPA/PostgreSQL (FASE 8), Security/JWT (FASE 11), Redis (FASE 12), RabbitMQ (FASE 13), WebFlux (FASE 14), Actuator/Micrometer (FASE 16).

## Testing

- JUnit 5 + Mockito. Cada clase de dominio con su test unitario (ver `testing.md`).