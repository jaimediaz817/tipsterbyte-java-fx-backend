---
name: java-documentation
description: Use when adding code comments, writing or updating docs, or logging decisions. Enforces the [QUÉ]/[POR QUÉ]/[ALTERNATIVAS]/[RELACIONES] standard and Spanish language across the repository.
---

# Java Documentation — tipsterbyte-fx-v2

## Cuándo usar

Al documentar artefactos codificados, escribir/actualizar docs, registrar decisiones (ADRs) o commits. Regla base: `.opencode/rules/documentation.md`.

## Estándar de cabecera

```java
// ─────────────────────────────────────────────
// [QUÉ]: Responsabilidad del artefacto en 1-2 líneas.
// [POR QUÉ]: Decisión de diseño que lo justifica.
// [ALTERNATIVAS]: (opcional) opciones descartadas y motivo.
// [RELACIONES]: Con qué casos de uso (CU-xx), puertos, adapters
//               o agregados de dominio se conecta.
// ─────────────────────────────────────────────
```

## Qué documentar

- **Clases/interfaces**: cabecera completa.
- **Métodos públicos no triviales**: 1-2 líneas de QUÉ/POR QUÉ.
- **Configuraciones** (application.yml, docker, CI): comentario breve de cada bloque significativo.
- **Decisiones técnicas**: entrada en `docs/architecture/arquitectura-objetivo.md` como ADR (Concepto / Artefacto / Ejemplo / Decisión con alternativas).

## Lenguaje y estilo

- Todo en **español** (docs, comentarios, commits).
- Comentarios que explican el porqué, no que repiten el código.
- Mensajes de commit: imperativo en español, una unidad lógica por commit.

## Trazabilidad exigida

- Código que implementa un caso de uso → referencia `CU-xx` en `[RELACIONES]`.
- Cambio que altera una regla de negocio → referencia `BR-xx` y actualizar `docs/domain/modelo-dominio.md`.