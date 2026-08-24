# 📣 Comunicado frontend — Poblamiento granular (HU-12)

> [QUÉ]: Contrato de los 2 pasos granulares para el panel Geografía: poblar solo países (#1) y poblar ligas de un país concreto (#5 + #6 si es de interés). Son el desglose del recorrido completo (`POST /catalogo/activar`) para tener visibilidad por etapa.
> [POR QUÉ]: El equipo frontend tiene dos botones que fallaban con `No static resource` (`geografia-panel.component.html:58` Poblar países → `POST /catalogo/poblar-paises`, `html:122` ⟳ por país → `POST /catalogo/poblar-ligas/{isoAlpha2}`). Este comunicado alinea el contrato híbrido acordado (países sync 200 + ligas por país async 202 con polling).
> [RELACIONES]: HU-12 → CU-17 `SincronizarPaisesUseCase` (sync) + CU-18 `SincronizarLigasPorPaisUseCase`/`SincronizarLigasPorPaisAsyncUseCase` (async por `isoAlpha2`) → `CatalogoController` (`/api/v1/catalogo/poblar-paises`, `/poblar-ligas/{isoAlpha2}`) + `GET /catalogo/activar/{executionId}` (polling T3) + `GET /catalogo/estado`.

---

## 1. Cuándo usar cada endpoint (hibrido)

| Paso | Endpoint | Método | Respuesta | Coste |
|---|---|---|---|---|
| 1 | `POST /api/v1/catalogo/poblar-paises` | `200 OK` síncrono, body vacío (`void`) | ~176 filas #1, sin scraping pesado (<2s). No requiere polling. Tras `200`, refresca `GET /catalogo/estado` para ver `totalPaises`. Ideal para el banner "Vacío". |
| 2 | `POST /api/v1/catalogo/poblar-ligas/{isoAlpha2}` | `202 Accepted` async `{executionId, estado:"RUNNING", urlEstado}` | Scraping #5 (`country_name` + `limit=maxLigasPorPais`) + temporadas + #6 por liga (si país es de interés). Puede tardar 30s-3min (ENG/ESP). Reutiliza el polling ya probado del recorrido completo (`GET /catalogo/activar/{executionId}` cada 5s). |

Por qué híbrido y no puro:
- Todo `200`: `poblar-ligas/{iso}` rompería el timeout 60s de `geografia-api.service.ts:177` en países grandes.
- Todo `202`: `poblar-paises` obligaría a polling innecesario para un paso trivial de segundos.

## 2. Endpoints

### `POST /api/v1/catalogo/poblar-paises` → `200`

- **Rol**: `SUPERADMIN` (403 resto, igual que `/activar`).
- **Body**: `{}` vacío (el frontend ya envía `{}` en `geografia-api.service.ts:162`).
- **Respuesta**: `200 OK` sin body (`void` → tu servicio ya espera `void` y muestra `toast geografia-panel.component.ts:178`). El backend ya invalidó `CacheClaves.paises()` para que `GET /paises/disponibles` vea frescos.
- **Errores**: `403` rol, `503` si #1 caído ( `InfraestructureException` ).
- **Post-acción frontend**: `GET /api/v1/catalogo/estado` → badge `POBLADO` + `totalPaises`.

```ts
// geografia-api.service.ts:162 — ya compatible
poblarPaises(): Observable<void> { return this.http.post<void>('/api/v1/catalogo/poblar-paises', {}); }
```

### `POST /api/v1/catalogo/poblar-ligas/{isoAlpha2}` → `202`

- **Rol**: `SUPERADMIN`.
- **Path**: `isoAlpha2` case-insensitive, 2 letras (`CO` == `co` → se normaliza a upper). Validación `DomainException` → `422` si `null`/vacío/no-2-letras.
- **Respuesta**:

```json
→ 202 Accepted
{
  "executionId": "uuid",
  "estado": "RUNNING",
  "urlEstado": "/api/v1/catalogo/activar/uuid"
}
```

Es el mismo shape `PoblamientoIniciadoResponse` de `POST /catalogo/activar` (`CatalogoController.java:54`), por lo que el `PoblarLigasResponse | null` de `geografia-api.service.ts:175` debe tiparse como ese shape o como `PoblamientoIniciadoResponse`. Si tu tipo espera `{ligasCreadas}` síncrono, cámbialo a este shape y usa polling.

- **Errores**:
  - `422` país no encontrado (`"País no encontrado para isoAlpha2: XX. Ejecuta primero POST /poblar-paises"`) — el panel debe mostrar "Primero pobla países".
  - `409` ya hay un poblamiento de **ese mismo `isoAlpha2`** en curso (`PoblamientoEnCursoException` → `GlobalExceptionHandler.java:48`), distinto del `409` global de `/activar` (all-countries). Permite poblar `CO` y `AR` en paralelo, pero no `CO` dos veces.
  - `403` rol.

- **Polling** (igual que recorrido completo): `GET /api/v1/catalogo/activar/{executionId}` `CatalogoController.java:67`

```json
→ 200 RUNNING { "estado":"RUNNING", "mensaje":"Poblamiento de ligas en curso para CO", "duracionMs": null }
→ 200 SUCCESS { "estado":"SUCCESS", "mensaje":"Ligas pobladas para CO: 3 nuevas, total 3", "duracionMs": 24000 }
→ 200 ERROR   { "estado":"ERROR", "mensaje":"...", "tipoError":"DomainException" }
→ 404 desconocido
```

Tras `SUCCESS`, refresca `GET /ligas?pais={nombre}&estado=BORRADOR` (ya incluye `totalEquipos`) y `GET /catalogo/estado` para `totalLigas`.

```ts
// ya tienes timeout 60-180s y manejo PoblarLigasResponse | null — cámbialo a:
poblarLigas(iso: string): Observable<PoblamientoIniciadoResponse> {
  return this.http.post<PoblamientoIniciadoResponse>(`/api/v1/catalogo/poblar-ligas/${iso}`, {});
}
// polling igual que activar:
poll(executionId) { return this.http.get<PoblamientoEstadoResponse>(`/api/v1/catalogo/activar/${executionId}`); }
```

## 3. Guía UI (Geografía)

```
┌─ Banner Vacío ──────────────────────────┐
│ Catálogo vacío — [Poblar países] (void) │ → POST /poblar-paises → 200 → toast + GET /estado
└────────────────────────────────────────┘
┌─ Por país ──────────────────────────────┐
│ 🇨🇴 Colombia  [⟳ Poblar ligas]         │ → POST /poblar-ligas/CO → 202 → polling badge "RUNNING" → SUCCESS toast "3 nuevas"
│ 🇪🇸 España    [⟳ Poblar ligas] 409 si CO en curso → toast "Ya hay un poblamiento en curso para CO"
└────────────────────────────────────────┘
```

- Deshabilita `[Poblar países]` mientras `RUNNING` global no aplica (es instantáneo).
- Deshabilita `[⟳]` de ese `iso` mientras su `executionId` está `RUNNING`; el resto sigue habilitado (paralelismo por país).
- `422` iso inválido/país no encontrado → tooltip "Primero pobla países".
- Usa `aria-live="polite"` en el texto de polling como en el recorrido completo.

## 4. Resumen status codes

| Operación | Éxito | Errores |
|---|---|---|
| `POST /poblar-paises` | `200` void | `403` rol · `503` #1 |
| `POST /poblar-ligas/{isoAlpha2}` | `202` + executionId | `422` iso/país no encontrado · `409` mismo iso en curso · `403` rol |
| `GET /activar/{executionId}` | `200` RUNNING/SUCCESS/ERROR | `404` desconocido |
| `GET /estado` | `200` VACIO/POBLADO | `403` rol |

---
*Fuente de verdad backend: `CatalogoController.java:30` (HU-12), `SincronizarPaisesUseCase.java` (CU-17), `SincronizarLigasPorPaisUseCase.java` + `SincronizarLigasPorPaisAsyncUseCase.java` (CU-18), `LigaRepository.java` `buscarPorPais`.*
