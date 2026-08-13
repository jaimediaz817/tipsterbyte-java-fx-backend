# Casos de Uso — tipsterbyte-fx-v2

> Núcleo pequeño de casos de uso que la FASE 6 implementará en la capa Application. Cada caso de uso orquesta: **controller → use case → dominio → puerto (port)**.

---

## Catálogo de casos de uso

| # | Caso de uso | Actor | Agrega valor a |
| --- | --- | --- | --- |
| CU-01 | Sincronizar tabla de posiciones | Sistema (fuente) | Liga |
| CU-02 | Sincronizar calendario de partidos | Sistema (fuente) | Liga / Partido |
| CU-03 | Sincronizar cuotas | Sistema (fuente) | Partido |
| CU-04 | Activar liga | Administrador | Liga |
| CU-05 | Registrar resultado de partido | Sistema / Administrador | Partido |
| CU-06 | Crear pronóstico | Tipster | Pronostico |
| CU-07 | Publicar pronóstico | Tipster | Pronostico |
| CU-08 | Consultar pronósticos por liga y fecha | Cliente | Pronostico |
| CU-09 | Crear suscripción | Cliente | Suscripcion |

---

## Detalle por caso de uso

### CU-01 — Sincronizar tabla de posiciones

**Actor**: Sistema (job programado, FASE 15) o Administrador (manual).
**Flujo**:
1. Obtener liga activa.
2. Consultar posiciones al **puerto de fuente de posiciones** (adapter → football-data.org o API-Football).
3. Mapear datos externos a `PosicionTabla` (VO).
4. Actualizar la tabla de posiciones de la `Liga`.
5. Persistir vía puerto de repositorio.

**Puertos usados**: `ProveedorPosiciones` (salida externa), `LigaRepository` (persistencia).
**Evento**: ninguno obligatorio.

### CU-02 — Sincronizar calendario de partidos

**Actor**: Sistema (job programado) o Administrador.
**Flujo**:
1. Obtener liga activa.
2. Consultar calendario al **puerto de fuente de calendario**.
3. Crear/actualizar `Partido`s (programados y jugados).
4. Emitir `PartidoProgramado` para cada partido nuevo.

**Puertos**: `ProveedorCalendario`, `PartidoRepository`.

### CU-03 — Sincronizar cuotas

**Actor**: Sistema (job programado) o Administrador.
**Flujo**:
1. Obtener partidos próximos de una liga.
2. Consultar cuotas al **puerto de fuente de cuotas** (API-Football / The Odds API / SharpAPI).
3. Actualizar `Cuota`s del partido (mercado + valor).
4. Emitir `CuotaActualizada`.

**Puertos**: `ProveedorCuotas`, `PartidoRepository`.

### CU-04 — Activar liga

**Actor**: Administrador.
**Regla aplicada**: BR-001 (solo se activa si las fuentes están configuradas y operativas).
**Flujo**:
1. Verificar configuración de fuentes de la liga.
2. Cambiar estado a `ACTIVA`.
3. Emitir `LigaActivada`.

### CU-05 — Registrar resultado de partido

**Actor**: Sistema (sincronización) o Administrador.
**Regla aplicada**: BR-003.
**Flujo**:
1. Localizar el partido.
2. Validar que el estado permita asignar resultado.
3. Asignar `Resultado` (VO) y cambiar estado a `FINALIZADO`.

### CU-06 — Crear pronóstico

**Actor**: Tipster.
**Reglas aplicadas**: BR-004 (partido programado), BR-007 (cuota > 1.0).
**Flujo**:
1. Elegir partido, mercado y selección.
2. Validar cuota vigente.
3. Crear `Pronostico` en estado `BORRADOR`.

### CU-07 — Publicar pronóstico

**Actor**: Tipster.
**Reglas aplicadas**: BR-005 (no editable una vez publicado).
**Flujo**:
1. Validar que el pronóstico esté en `BORRADOR`.
2. Cambiar estado a `PUBLICADO`.
3. Emitir `PronosticoPublicado`.

### CU-08 — Consultar pronósticos por liga y fecha

**Actor**: Cliente.
**Reglas aplicadas**: BR-006 (solo tipsters suscritos).
**Flujo**:
1. Validar suscripciones activas del cliente.
2. Consultar pronósticos publicados del tipster para liga/fecha.
3. Devolver DTO de respuesta.

### CU-09 — Crear suscripción

**Actor**: Cliente.
**Flujo**:
1. Elegir tipster y plan.
2. Crear `Suscripcion` en estado `ACTIVA`.
3. Emitir `SuscripcionCreada`.

---

## Trazabilidad de puertos (ports) usados

| Puerto | Dirección | Implementado por (FASE 8+) |
| --- | --- | --- |
| `LigaRepository` | Persistencia | Adapter JPA |
| `PartidoRepository` | Persistencia | Adapter JPA |
| `PronosticoRepository` | Persistencia | Adapter JPA |
| `SuscripcionRepository` | Persistencia | Adapter JPA |
| `ProveedorPosiciones` | Salida externa | football-data.org / API-Football |
| `ProveedorCalendario` | Salida externa | API-Football / football-data.org |
| `ProveedorCuotas` | Salida externa | API-Football / The Odds API / SharpAPI |