# Comunicado HU-FRONT-05 — Pantalla "Ligas (asignar equipos)"

> [QUÉ]: Contrato REST verificado contra el código real para la pantalla que dispara
>         la carga de equipos por liga y muestra la plantilla resultante.
> [POR QUÉ]: El equipo frontend necesita shapes y semántica exactos sin leer el
>             backend. Todo lo aquí escrito está extraído de `LigaController`,
>             `SincronizarEquiposLigaUseCase` (CU-16), `CatalogoController` y
>             `SecurityConfig`.
> [RELACIONES]: HU-11/HU-FRONT-05, CU-16, fuente #6 (`ext-soccerway-teams-by-league`),
>               comunicado-poblamiento-granular.md (flujo 202/polling, distinto a este).

## 1. Poblar equipos de UNA liga

```
POST /api/v1/ligas/{ligaId}/equipos/sincronizar?forzar=false
Authorization: Bearer <token>
Roles: SUPERADMIN | TIPSTER
Body: ninguno
```

| Query param | Default | Semántica |
|---|---|---|
| `forzar` | `false` | `false` = ruta rápida: si la plantilla ya tiene equipos, responde en milisegundos **sin consultar la fuente #6**. `true` = invalida cache y re-scrapea Python (actualiza escudos/datos; tarda minutos) |

Respuesta **200 OK** (SÍNCRONO — no hay 202 ni polling):

```json
{ "creados": 12, "actualizados": 8, "totalEquipos": 20, "desdePlantillaExistente": false }
```

| Campo | Tipo | Significado |
|---|---|---|
| `creados` | int | Equipos nuevos insertados en la plantilla |
| `actualizados` | int | Equipos existentes cuyo escudo se refrescó |
| `totalEquipos` | int | Tamaño total de la plantilla tras la sync (badge "28/30") |
| `desdePlantillaExistente` | bool | `true` = NO se consultó la fuente: la plantilla ya tenía equipos y no se pidió forzar (`creados=0`, `actualizados=0`). El botón puede mostrar "Ya tiene N equipos" sin spinner largo |

### Consideraciones UX obligatorias

- Con `forzar=false` sobre plantilla poblada la respuesta es inmediata: el spinner solo
  aparece en la primera carga (plantilla vacía) o con `forzar=true` (scrape real,
  decenas de segundos → timeout generoso + deshabilitar botón durante el request).
- Anti-solapamiento server-side por liga: dos clicks simultáneos → el segundo recibe
  **409 Conflict** con body `ApiError` ("Ya hay una sincronización de equipos en curso...").
  Igual recomendamos deshabilitar el botón durante el request.

### Errores adicionales

| Status | Situación |
|---|---|
| 409 | Ya existe una sincronización de equipos en curso para esa liga |

## 2. Listado de la plantilla

```
GET /api/v1/ligas/{ligaId}/equipos
Authorization: Bearer <token>
Roles: SUPERADMIN | TIPSTER
```

Respuesta **200 OK**:

```json
{
  "ligaId": "uuid",
  "temporadaId": "uuid",
  "temporadaNombre": "2025/2026",
  "temporadaEstado": "PLANIFICADA",
  "total": 20,
  "equipos": [
    { "id": "uuid", "nombre": "Atlético Nacional", "logoUrl": "https://..." }
  ]
}
```

Notas:

- El campo del escudo es **`logoUrl`** (no `urlBandera`).
- La plantilla corresponde a la **temporada vigente**: preferencia temporada ACTIVA;
  si no hay, la primera registrada (catálogo recién creado → PLANIFICADA).
- Endpoint complementario de diagnóstico:
  `GET /api/v1/ligas/{ligaId}/equipos/discrepancias` → pares sospechosos de
  duplicado (H-04). SOLO detecta: nunca fusiona ni elimina.

## 3. Semántica de re-ejecución (idempotencia)

- **Incremental e idempotente**: matching por nombre normalizado; crea solo los
  nuevos, actualiza escudos si cambiaron, **NUNCA elimina** equipos existentes.
- Sin `forzar`, la re-ejecución sobre plantilla poblada es una **no-op rápida**
  (`desdePlantillaExistente=true`): la fuente #6 solo se consulta si la plantilla
  está vacía o si el usuario pide explícitamente `?forzar=true` (actualizar escudos).

### Errores (todos → 422 con body `ApiError`, ver GlobalExceptionHandler)

| Situación | Mensaje |
|---|---|
| Liga inexistente | `Liga no encontrada: {id}` |
| Liga sin temporadas registradas | `La liga no tiene temporadas registradas: {id}` |
| Fuente #6 devolvió vacía | `La fuente #6 no devolvió equipos para '<pais>' / '<nombre>'` |

401/403 responden `ApiError` JSON igualmente (ApiErrorAuthenticationEntryPoint /
ApiErrorAccessDeniedHandler).

## 4. Pendientes heredados resueltos

### ¿GET /ligas sin filtros devolviendo [] cuando solo hay BORRADOR es intencional?

**Sí, intencional.** Sin filtros devuelve solo las **ACTIVA** (selector de ligas
del tipster). Para el panel admin usar filtros explícitos:

```
GET /api/v1/ligas?estado=BORRADOR
GET /api/v1/ligas?estado=BORRADOR&pais=Colombia   (nombre país exacto, case-insensitive)
```

### ¿GET /catalogo/activar/{executionId} tiene efectos secundarios si el id no existe?

**No. Es lectura pura** sobre `TareaLog` + snapshot en memoria; id inexistente →
**404 Not Found** limpio, sin efectos. Recordar: `/api/v1/catalogo/**` exige rol
**SUPERADMIN** exclusivo (los endpoints de ligas admiten SUPERADMIN o TIPSTER).
