# Modelo de Dominio (DDD) — tipsterbyte-fx-v2

> Este documento define el vocabulario del dominio (Ubiquitous Language) y su modelado DDD. Es la base que la FASE 5 implementará en Java.

---

## Diagrama de contexto

```
                    ┌────────────────────────────┐
                    │      Plataforma            │
                    │   Pronósticos Fútbol       │
                    └────────────┬───────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
   ┌────▼────┐            ┌─────▼──────┐           ┌──────▼─────┐
   │  Liga   │            │  Partido   │           │ Pronostico │
   └─────────┘            └────────────┘           └────────────┘
   (aggregate)            (aggregate)               (aggregate)
        │                        │                        │
   ┌────▼────┐            ┌─────▼──────┐           ┌──────▼─────┐
   │ Equipo  │            │   Cuota    │           │ Suscripcion│
   └─────────┘            └────────────┘           └────────────┘
        │
   ┌────▼────┐
   │  Pais   │  (catálogo · CU-10 · FASE 8.5)
   └─────────┘
```

---

## Ubiquitous Language (glosario)

| Término | Significado |
| --- | --- |
| **Liga** | Competición deportiva de fútbol (ej: Premier League, Liga BetPlay). |
| **Equipo** | Club que participa en una liga. |
| **Partido** | Enfrentamiento entre dos equipos en una fecha. |
| **Cuota** | Multiplicador de apuesta para un mercado (1X2, over/under, etc.). |
| **Mercado** | Tipo de apuesta (resultado, doble oportunidad, etc.). |
| **Pronóstico** | Opinión del tipster sobre un partido con una selección y una cuota. |
| **Suscripción** | Relación paga entre un cliente y un tipster. |
| **Fuente de datos** | Proveedor externo (API/scraper) que entrega datos deportivos. |
| **Activación de liga** | Proceso de asociar las fuentes disponibles a una liga para iniciar extracción. |

---

## Aggregates

### Aggregate 1 — Liga

**Aggregate Root**: `Liga`

> **Actualizado (refactor Torneos/Temporadas)**: `Temporada` es Entity con identidad propia y es el centro del modelo deportivo — compone los equipos y la tabla de posiciones (un equipo que desciende en 2024 no está en la tabla 2025). `Liga` delega en su *temporada vigente* (activa o, en su defecto, la primera registrada). `Partido` y `DetalleFuenteExtraccion` referencian `temporadaId`; `ligas.pais_id` es FK real al catálogo `Pais`. Detalle completo en `docs/REFactor-Torneos-Temporadas.md`.

| Miembro | Tipo |
| --- | --- |
| `id` | Identity |
| `nombre` | Value Object |
| `pais` | Value Object (nombre denormalizado; FK real `paisId` ↔ catálogo `Pais`) |
| `temporadas` | `Set<Temporada>` — **Entity** con identidad (1:N) |
| `urlSoccerway` | Opcional — path_to_scrape del calendario (fuente #5) |
| `apiId` | Opcional — id en API-Football (fuente #5) |
| `estado` | enum: `BORRADOR`, `ACTIVA`, `INACTIVA` |

**Miembros de `Temporada`**:

| Miembro | Tipo |
| --- | --- |
| `id` / `ligaId` | Identity + referencia al aggregate padre |
| `nombre` | Opcional — nombre del torneo (ej: "Apertura", "Clausura") |
| `semestre` | Opcional — 1 o 2 |
| `anioInicio` / `anioFin` | rango válido: fin > inicio |
| `estado` | enum: `PLANIFICADA`, `ACTIVA`, `FINALIZADA` |
| `equipos` | Lista de `Equipo` (Entity) — **de la temporada** |
| `posiciones` | Tabla de posiciones (`List<PosicionTabla>`) — **de la temporada** |

**Reglas del aggregate**:
- Una liga solo puede **activarse** si tiene al menos una fuente de datos configurada y operativa.
- Las posiciones se recalculan cuando llega la sincronización de la fuente, sobre la temporada vigente.
- Los equipos se agregan/desagregan desde la sincronización de posiciones/calendario, en la temporada vigente.

### Aggregate 2 — Partido

**Aggregate Root**: `Partido`

| Miembro | Tipo |
| --- | --- |
| `id` | Identity |
| `liga` | Referencia (id) |
| `equipoLocal` | `Equipo` |
| `equipoVisitante` | `Equipo` |
| `fechaProgramada` | Value Object (`FechaProgramada`) |
| `resultado` | Value Object (`Resultado`) — opcional hasta que finaliza |
| `cuotas` | `List<Cuota>` |
| `estado` | enum: `PROGRAMADO`, `EN_VIVO`, `FINALIZADO`, `SUSPENDIDO` |

**Reglas del aggregate**:
- El resultado solo se asigna cuando el partido finaliza.
- Las cuotas se actualizan desde la fuente de odds (nunca se mutan localmente).
- Un pronóstico solo puede referenciar un partido `PROGRAMADO` o `EN_VIVO`.

### Aggregate 3 — Pronostico

**Aggregate Root**: `Pronostico`

| Miembro | Tipo |
| --- | --- |
| `id` | Identity |
| `tipster` | Referencia (id) |
| `partido` | Referencia (id) |
| `mercado` | Value Object (`Mercado`) |
| `seleccion` | Value Object (`SeleccionPronostico`) |
| `cuota` | Value Object (`Cuota`) |
| `estado` | enum: `BORRADOR`, `PUBLICADO`, `ANULADO` |
| `resultadoFinal` | Opcional — verificación del pronóstico |

**Reglas del aggregate**:
- Un pronóstico en `BORRADOR` no es visible para clientes.
- Solo se publica si el partido está programado y la cuota es vigente.
- Un pronóstico no se puede modificar una vez publicado (se anula o se deja).

### Aggregate 4 — Suscripcion

**Aggregate Root**: `Suscripcion`

| Miembro | Tipo |
| --- | --- |
| `id` | Identity |
| `cliente` | Referencia (id) |
| `tipster` | Referencia (id) |
| `plan` | Value Object (`Plan`) |
| `fechaInicio` / `fechaFin` | Fechas |
| `estado` | enum: `ACTIVA`, `CANCELADA`, `EXPIRADA` |

**Reglas del aggregate**:
- Un cliente solo ve pronósticos publicados por tipsters a los que está suscrito.
- La suscripción expira al llegar `fechaFin`.
- (Pagos entran en FASE 11/13 de forma simulada.)

---

## Entities

- **Equipo**: tiene identidad propia (id), pertenece a una liga. Cambia (nombre, estadio, etc.) pero mantiene su identidad.
- **Pais** (catálogo, FASE 8.5): tiene identidad propia (id) y vive fuera de los aggregates. Es la tabla maestra de países del catálogo (CU-10, fuente #1) con `nombre`, `isoAlpha2`, `continente`, `code`, `href`, `mapeado`. `Liga.pais` conserva el nombre denormalizado.
- **Tipster**: entidad usuario con rol que crea pronósticos.
- **Cliente**: entidad usuario que consume pronósticos mediante suscripciones.

> Auth/Users se profundiza en FASE 11 (Spring Security + JWT). Aquí solo se modela la identidad mínima necesaria para el negocio.

---

## Value Objects

| VO | Atributos | Reglas |
| --- | --- | --- |
| `Resultado` | `golesLocal`, `golesVisitante` | >= 0 |
| `Cuota` | `valor` (decimal) | valor > 1.0 |
| `PosicionTabla` | `equipo`, `posicion`, `jugados`, `ganados`, `empatados`, `perdidos`, `golesFavor`, `golesContra`, `puntos` | consistencia aritmética |
| `Mercado` | `nombre` (1X2, doble oportunidad, over/under...) | enum cerrado |
| `SeleccionPronostico` | `mercado`, `resultadoEsperado` | válido para el mercado |
| `Email` | `direccion` | formato válido |
| `Rol` | `CLIENTE`, `TIPSTER`, `SUPERADMIN` | enum cerrado |
| `Plan` | `nombre`, `precio`, `duracionDias` | precio >= 0 |

---

## Domain Events

| Evento | Cuándo ocurre | Interesados futuros |
| --- | --- | --- |
| `LigaActivada` | Se activa una liga con fuentes listas | Scheduler de ingesta (FASE 15), notificaciones (FASE 13) |
| `PartidoProgramado` | Se sincroniza un partido desde el calendario | Cache de calendario (FASE 12) |
| `CuotaActualizada` | La fuente de odds entrega nuevas cuotas | Cache de cuotas (FASE 12), pronósticos |
| `PronosticoPublicado` | Un tipster publica un pronóstico | Notificaciones a suscriptores (FASE 13) |
| `SuscripcionCreada` | Un cliente se suscribe | Facturación/notificación (FASE 13) |

---

## Reglas de negocio consolidadas

1. **BR-001**: Una liga solo se activa cuando las fuentes de datos (posiciones, calendario, cuotas) están configuradas y operativas.
2. **BR-002**: La extracción de datos no se inicia para ligas inactivas.
3. **BR-003**: El resultado de un partido solo se asigna cuando el estado es `FINALIZADO`.
4. **BR-004**: Un pronóstico solo se publica sobre partidos programados con cuota vigente.
5. **BR-005**: Un pronóstico publicado no se edita; solo se anula.
6. **BR-006**: Un cliente solo consume pronósticos de tipsters con suscripción activa.
7. **BR-007**: Una cuota siempre es > 1.0; si la fuente entrega valores inválidos, se descarta.
8. **BR-008**: Las posiciones deben mantener consistencia (puntos = 3*ganados + 1*empatados).