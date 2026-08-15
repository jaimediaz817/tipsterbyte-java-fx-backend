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
| CU-10 | Sincronizar catálogo de países y ligas | Sistema (fuente) | Pais / Liga |
| CU-11 | Gestionar fuentes de extracción | Administrador | FuenteExtraccion / DetalleFuenteExtraccion |

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
**Flujo (FASE 8.5 paso 2)**:
1. Recibir `ActivarLigaComando` con las 3 URLs reales de la liga: `urlPosiciones`, `urlCalendario`, `urlCuotas`.
2. Buscar la fuente activa por tipo (`STANDINGS`, `CALENDAR`, `ODDS_WPLAY`).
3. Crear un `DetalleFuenteExtraccion` por tipo con su URL (asociación liga↔fuente↔URL).
4. Derivar la disponibilidad de cada fuente según si su URL está presente.
5. Llamar `liga.activar(...)` → valida BR-001 y cambia a `ACTIVA`.
6. Emitir `LigaActivada` y persistir liga + detalles.

**Puertos**: `LigaRepository`, `FuenteExtraccionRepository`, `DetalleFuenteExtraccionRepository`.
**DTO**: `ActivarLigaComando` (nuevo, con las 3 URLs).

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

### CU-10 — Sincronizar catálogo de países y ligas

**Actor**: Sistema (fuentes reales `#1` y `#5`).
**Agregados**: `Pais` (nuevo) / `Liga`.
**Propósito**: poblar el catálogo base (países y ligas por país) para que luego las ligas candidatas puedan activarse (CU-04) y sincronizarse (CU-01/02/03).

**Flujo**:
1. Consultar países al endpoint `#1` (`/ext-soccerway-countries`) → `PaisFuente`.
2. Persistir países nuevos en el catálogo.
3. Por cada país, consultar ligas al endpoint `#5` (`/ext-soccerway-leagues-by-country?country_name=...&limit=...`) → `LigaFuente`.
4. Mapear a `Liga` en estado `BORRADOR`, con `pais`, `temporada` (del campo `anio`), `urlSoccerway` (candidato a `path_to_scrape` de calendario) y `apiId` si viene.
5. Persistir ligas nuevas; no duplicar las ya existentes.

**Regla aplicada**: solo crea ligas en `BORRADOR`; la activación real queda para CU-04 (requiere fuentes operativas).

**Puertos**: nuevo `PaisRepository` (catálogo) + `LigaRepository` existente.
**DTOs**: `PaisFuente`, `LigaFuente` (nuevos en `application.dto`).
**Fuentes**: adapters `SoccerwayPaisesAdapter` (#1) y `SoccerwayLigasPorPaisAdapter` (#5).

### CU-11 — Gestionar fuentes de extracción

**Actor**: Administrador.
**Agregados**: `FuenteExtraccion` / `DetalleFuenteExtraccion` (nuevos en `domain.model`).
**Propósito**: administrar el catálogo de fuentes (qué fuente sirve cada tipo de dato) y asociar a cada liga la URL real (`path_to_scrape`) de la fuente que la alimenta. Estas URLs son la entrada que los adapters HTTP usan para consultar posiciones, calendario y cuotas.

**Flujo**:
1. **Registrar fuente** (`registrarFuente`): crea `FuenteExtraccion` con nombre, tipo (`TipoFuenteExtraccion`) y estado; un solo registro por tipo (validación de unicidad).
2. **Listar fuentes** (`listarFuentes`): devuelve el catálogo de fuentes del sistema.
3. **Asociar URL** (`asociarUrlFuente`): crea o actualiza el `DetalleFuenteExtraccion` de la liga para ese tipo, guardando la URL real (no duplica si ya existe).
4. **Listar detalles de liga** (`listarDetallesDeLiga`): devuelve las fuentes + URLs asociadas a una liga.

**Reglas aplicadas**: un solo `DetalleFuenteExtraccion` por (liga, tipo) — se actualiza si ya existe.

**Puertos**: `FuenteExtraccionRepository`, `DetalleFuenteExtraccionRepository` (nuevos).
**DTOs**: `RegistrarFuenteComando`, `AsociarUrlFuenteComando`, `FuenteExtraccionResponse`.
**Endpoints REST**: `POST /api/v1/fuentes`, `GET /api/v1/fuentes`, `PUT /api/v1/ligas/{ligaId}/fuentes/{tipo}`, `GET /api/v1/ligas/{ligaId}/fuentes`.

---

## Trazabilidad de puertos (ports) usados

| Puerto | Dirección | Implementado por (FASE 8+) |
| --- | --- | --- |
| `LigaRepository` | Persistencia | Adapter JPA |
| `PartidoRepository` | Persistencia | Adapter JPA |
| `PronosticoRepository` | Persistencia | Adapter JPA |
| `SuscripcionRepository` | Persistencia | Adapter JPA |
| `PaisRepository` | Persistencia | Adapter JPA |
| `FuenteExtraccionRepository` | Persistencia | Adapter JPA |
| `DetalleFuenteExtraccionRepository` | Persistencia | Adapter JPA |
| `ProveedorPosiciones` | Salida externa | FlashscorePosicionesAdapter (fuente `#3`) |
| `ProveedorCalendario` | Salida externa | SoccerwayCalendarioAdapter (fuente `#4`) |
| `ProveedorCuotas` | Salida externa | WplayCuotasAdapter (fuente `#2`) |