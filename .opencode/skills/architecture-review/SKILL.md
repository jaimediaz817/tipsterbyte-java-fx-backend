---
name: architecture-review
description: Use to audit the repository at any milestone, especially FASE 21. Reviews Clean Architecture, SOLID, DDD, dependencies, coupling, testing, security, observability and produces CRITICAL / WARNING / OBSERVATION / GOOD / RECOMMENDATION findings.
---

# Architecture Review — tipsterbyte-fx-v2

## Cuándo usar

Auditoría formal de arquitectura. Se ejecuta al cierre de hitos y como fase dedicada en FASE 21 (`docs/PROYECTO-PLAN.md`).

## Áreas a revisar

- Clean Architecture / Dependency Rule
- SOLID (SRP, OCP, ISP, DIP, LSP)
- DDD (aggregates, VOs, reglas de negocio BR-xx, eventos)
- Dependencias (acoplamiento, duplicación DRY)
- Cohesión / responsabilidad de clases
- Testing (pirámide: suficiencia de unit tests, ubicación de integration)
- Security (secretos, validación, autenticación)
- Observabilidad (logs, metrics, health)
- Naming y documentación (rules `naming.md`, `documentation.md`)

## Formato de salida

Clasificar cada hallazgo en:

- **CRITICAL**: rompe Dependency Rule, expone secretos, regla de negocio sin test. Bloquea avance.
- **WARNING**: riesgo latente (ej: VO mutable, acoplamiento excesivo).
- **OBSERVATION**: detalle de estilo o claridad.
- **GOOD**: decisión que se debe mantener y defender en entrevista.
- **RECOMMENDATION**: mejora opcional con prioridad.

## Cierre de revisión

1. Lista de hallazgos con referencia exacta (archivo:línea).
2. Plan de remediación priorizado (CRITICAL primero).
3. Actualizar `docs/architecture/arquitectura-objetivo.md` con ADRs nuevos.
4. Resumen defendible: "por qué esta arquitectura es correcta" para FASE 22.