# Historias de Usuario — tipsterbyte-fx-v2

> Complemento a `casos-de-uso.md`. Las HU responden *qué* y *por qué* desde la perspectiva del actor; los casos de uso responden *cómo*. Cada HU tiene trazabilidad hacia su caso de uso.

---

## Formato

```
Como [actor],
quiero [capacidad],
para [beneficio/valor].
```

**Criterios de aceptación (AC)**: condiciones verificables que deben cumplirse para considerar la HU terminada.

---

## Épicas

| Épica | HU asociadas |
| --- | --- |
| Ingesta de datos deportivos | HU-01 a HU-04 |
| Gestión de partidos | HU-05 |
| Pronósticos | HU-06 a HU-08 |
| Suscripciones | HU-09 |

---

## HU-01 — Sincronizar tabla de posiciones

**Como** administrador, **quiero** sincronizar la tabla de posiciones de una liga activa, **para** que los pronósticos se basen en datos actualizados.

**Criterios de aceptación**:
- AC1: La sincronización se dispara por job programado o manualmente.
- AC2: Los datos provienen de un adapter de fuente de posiciones (football-data.org o API-Football).
- AC3: Las posiciones guardadas respetan consistencia aritmética (BR-008).
- AC4: Se registra el resultado de la extracción (éxito/fallo).

**Trazabilidad**: → CU-01 → `ProveedorPosiciones` + `LigaRepository`

---

## HU-02 — Sincronizar calendario de partidos

**Como** administrador, **quiero** sincronizar el calendario de partidos jugados y pendientes de una liga, **para** tener partidos programados y resultados históricos.

**Criterios de aceptación**:
- AC1: Se crean partidos nuevos en estado `PROGRAMADO`.
- AC2: Se actualizan resultados de partidos ya jugados.
- AC3: Cada partido nuevo emite `PartidoProgramado`.

**Trazabilidad**: → CU-02 → `ProveedorCalendario` + `PartidoRepository`

---

## HU-03 — Sincronizar cuotas

**Como** administrador, **quiero** sincronizar las cuotas de los partidos próximos de una liga, **para** que los tipsters publiquen pronósticos con odds vigentes.

**Criterios de aceptación**:
- AC1: Se actualizan cuotas solo de partidos `PROGRAMADO` o `EN_VIVO`.
- AC2: Cuotas inválidas (≤ 1.0) se descartan (BR-007).
- AC3: Se emite `CuotaActualizada` por cada partido actualizado.

**Trazabilidad**: → CU-03 → `ProveedorCuotas` + `PartidoRepository`

---

## HU-04 — Activar liga

**Como** administrador, **quiero** activar una liga solo cuando sus fuentes de datos estén operativas, **para** no iniciar extracción de datos incompletos.

**Criterios de aceptación**:
- AC1: La activación valida que las fuentes de la liga estén configuradas (BR-001).
- AC2: Si falta alguna fuente, la liga permanece `INACTIVA` con mensaje claro.
- AC3: Al activarse se emite `LigaActivada`.

**Trazabilidad**: → CU-04 → `LigaRepository`

---

## HU-05 — Registrar resultado de partido

**Como** administrador, **quiero** registrar el resultado final de un partido, **para** marcar el partido como `FINALIZADO` y permitir verificar pronósticos.

**Criterios de aceptación**:
- AC1: Solo se asigna resultado a partidos `EN_VIVO` o `PROGRAMADO` ya jugados (BR-003).
- AC2: Una vez `FINALIZADO`, el resultado no se modifica.

**Trazabilidad**: → CU-05 → `PartidoRepository`

---

## HU-06 — Crear pronóstico

**Como** tipster, **quiero** crear un pronóstico en borrador, **para** prepararlo sin exponerlo a los clientes.

**Criterios de aceptación**:
- AC1: El partido debe estar `PROGRAMADO` o `EN_VIVO` (BR-004).
- AC2: La cuota seleccionada es vigente y > 1.0 (BR-007).
- AC3: El pronóstico nace en estado `BORRADOR`.

**Trazabilidad**: → CU-06 → `PronosticoRepository` + `PartidoRepository`

---

## HU-07 — Publicar pronóstico

**Como** tipster, **quiero** publicar un pronóstico, **para** que mis suscriptores puedan verlo.

**Criterios de aceptación**:
- AC1: Solo se publica un pronóstico en `BORRADOR` (BR-005).
- AC2: Al publicarse se emite `PronosticoPublicado`.
- AC3: Un pronóstico publicado no se edita; solo se anula.

**Trazabilidad**: → CU-07 → `PronosticoRepository`

---

## HU-08 — Consultar pronósticos por liga y fecha

**Como** cliente, **quiero** consultar los pronósticos publicados por mis tipsters suscritos según liga y fecha, **para** decidir mis apuestas.

**Criterios de aceptación**:
- AC1: Solo se muestran pronósticos de tipsters con suscripción activa (BR-006).
- AC2: Solo se muestran pronósticos `PUBLICADO`.
- AC3: La respuesta incluye mercado, selección y cuota.

**Trazabilidad**: → CU-08 → `PronosticoRepository` + `SuscripcionRepository`

---

## HU-09 — Crear suscripción

**Como** cliente, **quiero** suscribirme a un tipster con un plan, **para** acceder a sus pronósticos.

**Criterios de aceptación**:
- AC1: Se crea una suscripción `ACTIVA` con fechas y plan.
- AC2: Se emite `SuscripcionCreada`.
- AC3: La suscripción expira al llegar `fechaFin`.

**Trazabilidad**: → CU-09 → `SuscripcionRepository`

---

## Matriz de trazabilidad HU → CU → Puertos

| HU | Caso de uso | Puertos |
| --- | --- | --- |
| HU-01 | CU-01 | `ProveedorPosiciones`, `LigaRepository` |
| HU-02 | CU-02 | `ProveedorCalendario`, `PartidoRepository` |
| HU-03 | CU-03 | `ProveedorCuotas`, `PartidoRepository` |
| HU-04 | CU-04 | `LigaRepository` |
| HU-05 | CU-05 | `PartidoRepository` |
| HU-06 | CU-06 | `PronosticoRepository`, `PartidoRepository` |
| HU-07 | CU-07 | `PronosticoRepository` |
| HU-08 | CU-08 | `PronosticoRepository`, `SuscripcionRepository` |
| HU-09 | CU-09 | `SuscripcionRepository` |