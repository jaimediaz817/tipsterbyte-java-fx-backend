# 📣 Comunicado frontend — Poblamiento geográfico (disparo manual)

> [QUÉ]: Contrato del disparo manual del poblamiento geográfico: un botón que ejecuta el recorrido completo **países → ligas → torneos (temporadas)** contra las fuentes reales (#1/#5), respetando los países de interés y sus límites `maxLigasPorPais`.
> [POR QUÉ]: El equipo frontend necesita habilitar el "momento 0" del panel de administración: poblar el catálogo manualmente y visualizar su estado. La alternativa programada (tarea global) se documenta en un comunicado aparte.
> [RELACIONES]: CU-10 `SincronizarCatalogoUseCase` → `CatalogoController` (`POST /api/v1/catalogo/activar` + `GET /api/v1/catalogo/estado`). Requiere rol SUPERADMIN. Consume lo configurado en CU-14 (`/paises-interes`).

---

## 1. ¿Qué hace el poblamiento? (para el tooltip/ayuda del botón)

Al pulsar el botón, el backend ejecuta **una sola pasada síncrona**:

1. Lee los ~176 países de la fuente #1 y los persiste si son nuevos.
2. Ordena el recorrido: **primero los países de interés** (por prioridad, tal como están en "Mis preferidos"), después el resto del mundo (nunca se omite ninguno).
3. Por cada país consulta la fuente #5 y registra sus ligas nuevas; si el país tiene `maxLigasPorPais`, extrae como máximo esas ligas.
4. Cada liga nueva nace con **su temporada de catálogo** derivada del año (ej: "2026/2027", estado PLANIFICADA) y con FK real a su país.

Es **idempotente**: pulsarlo dos veces no duplica nada (países por ISO, ligas por URL). Sirve tanto para la primera población como para refrescar/incorporar ligas nuevas.

## 2. Dónde va el botón (recomendación)

- **Ubicación**: pantalla de administración de catálogo/poblamiento (donde ya viven "Mis preferidos" / países de interés), visible solo para rol **SUPERADMIN**.
- **Etiqueta sugerida**: "Poblar catálogo" o "Sincronizar geografía ahora".
- **Estado inicial**: al cargar la pantalla, consultar `GET /catalogo/estado` para mostrar si el catálogo está vacío o poblado (y con cuántos países/ligas).
- **Durante la ejecución**: el endpoint es **síncrono y puede tardar varios minutos** (recorre el mundo scrapeando). Recomendaciones:
  - Deshabilitar el botón + spinner/indicador de progreso indeterminado mientras espera la respuesta.
  - Mensaje tipo "Esto puede tardar unos minutos".
  - No cerrar/navegar fuera: la ejecución continúa en el backend aunque el cliente corte, pero perderás la respuesta final.
- **Tras la respuesta**: refrescar el estado con el body de la respuesta (ya trae conteos actualizados).

## 3. Endpoints

> ⚠️ **FASE T3 — el disparo es ASÍNCRONO**: el poblamiento corre en background y se monitorea por polling. Ya no hay respuesta síncrona con conteos.

### `POST /api/v1/catalogo/activar`

- **Rol**: SUPERADMIN (403 para otros roles).
- Lanza el poblamiento en background y responde **inmediatamente**:

```json
→ 202 Accepted
{
  "executionId": "uuid-de-ejecucion",
  "estado": "RUNNING",
  "urlEstado": "/api/v1/catalogo/activar/uuid-de-ejecucion"
}
```

- **`409 Conflict`** si ya hay un poblamiento en curso (`{mensaje}`): mostrar "Espera o consulta el estado del poblamiento actual" y ofrecer ir al polling.

### `GET /api/v1/catalogo/activar/{executionId}` — polling

```json
→ 200 OK (RUNNING — con progreso)
{
  "executionId": "uuid",
  "estado": "RUNNING",
  "paisActual": "Colombia",
  "paisesProcesados": 12,
  "fechaInicio": "2026-08-22T05:00:00Z",
  "duracionMs": null,
  "mensaje": "Poblamiento geográfico manual en curso"
}

→ 200 OK (SUCCESS — final, sin paisActual)
{
  "executionId": "uuid",
  "estado": "SUCCESS",
  "paisActual": null,
  "paisesProcesados": null,
  "fechaInicio": "...",
  "duracionMs": 240000,
  "mensaje": "Poblamiento geográfico manual completado"
}
```

| Campo | RUNNING | SUCCESS | ERROR |
|---|---|---|---|
| `paisActual` / `paisesProcesados` | ✅ snapshot en vivo | `null` | `null` |
| `duracionMs` | `null` | ✅ total | ✅ hasta el fallo |
| `mensaje` | "en curso" | "completado" | causa del error |

- `404` si el executionId no existe.
- Tras `SUCCESS`, refrescar `GET /catalogo/estado` para los conteos finales.

### `GET /api/v1/catalogo/estado`

- **Rol**: SUPERADMIN.
- Estado actual sin ejecutar nada:

| Campo | Tipo | Valores |
|---|---|---|
| `estado` | string | `VACIO` / `POBLADO` |
| `totalPaises` | int | conteo real en BD |
| `totalLigas` | int | conteo real en BD |

Útil para: mostrar badge "Catálogo vacío" al primer arranque, o "X países · Y ligas" cuando ya está poblado.

## 4. Guía UI/UX del botón (actualizada a async)

1. **Clic en "Poblar catálogo"** → `POST /activar`.
   - `202` → el botón pasa a estado "Ejecutando…" (disabled) y arranca **polling cada 5 s** sobre `urlEstado`.
   - `409` → toast *"Ya hay un poblamiento en curso"* + botón para consultar su progreso.
2. **Mientras RUNNING**: barra de progreso indeterminada + texto dinámico `"Procesando: Colombia (12 países)"` con `paisActual`/`paisesProcesados`. El usuario puede navegar libremente (el proceso sigue en el backend).
3. **Al recibir SUCCESS**: toast con duración ("Poblamiento completado en 4 min"), refrescar `GET /catalogo/estado`, rehabilitar botón.
4. **Al recibir ERROR**: toast rojo con `mensaje`, sugerir reintentar (es idempotente).
5. **Accesibilidad**: `aria-live="polite"` en el texto de progreso; mantener el estado del botón si el usuario recarga la página (al cargar, consultar si hay ejecución reciente — v2: endpoint de "última ejecución").

## 4b. Resumen de status codes (disparo)

| Operación | Éxito | Errores |
|---|---|---|
| `POST /catalogo/activar` | `202` + executionId | `409` en curso · `403` rol |
| `GET /catalogo/activar/{executionId}` | `200` | `404` desconocido |
| `GET /catalogo/estado` | `200` | `403` rol |

## 5. Relación con otras pantallas

- **"Mis preferidos" (países de interés)**: configura QUÉ países van primero y CUÁNTAS ligas se extraen de cada uno. El poblamiento respeta esa configuración en cada corrida — no hay que volver a guardarla para que aplique.
- **Tareas programadas**: la misma automatización podrá dejarse programada creando una tarea global de catálogo (se detalla en el próximo comunicado de tareas programadas).

## 6. ⭐ Nueva sección: "Ligas de mis países de interés" (plantillas con escudos)

El poblamiento ahora deja cada liga de tus países de interés **con su plantilla de equipos (nombre + escudo)**. Para dar visibilidad y control sobre ese paso:

### `GET /api/v1/ligas?pais={nombre}&estado=BORRADOR` → ahora incluye `totalEquipos`

```json
{
  "id": "uuid",
  "nombre": "Liga Betplay",
  "pais": "Colombia",
  "estado": "BORRADOR",
  "temporada": "2026/2027",
  "totalEquipos": 28,
  "urlSoccerway": "...",
  "apiId": null
}
```

> `totalEquipos` = tamaño de la plantilla de la temporada vigente. El total esperado NO lo provee el backend (viene de la fuente al poblar): el frontend puede mostrar solo el número, o comparar contra el último valor conocido.

### `GET /api/v1/ligas/{ligaId}/equipos` ⭐ nuevo (H-03)

La plantilla completa de la temporada vigente, con escudos:

```json
→ 200 OK
{
  "ligaId": "uuid",
  "temporadaId": "uuid",
  "temporadaNombre": "2026/2027",
  "temporadaEstado": "PLANIFICADA",
  "total": 28,
  "equipos": [
    { "id": "uuid", "nombre": "Millonarios", "logoUrl": "https://static.flashscore.com/...png" },
    { "id": "uuid", "nombre": "Atlético Nacional", "logoUrl": null }
  ]
}
```

- `logoUrl` puede ser `null` si el equipo entró por fuentes operativas (#3/#4) sin escudo.
- `422` si la liga no existe o no tiene temporadas.
- Ideal para: expandir la fila de la tabla ("ver plantilla"), modal con grid de escudos, o pantalla de detalle.

### `GET /api/v1/ligas/{ligaId}/equipos/discrepancias` ⭐ nuevo (H-04)

Diagnóstico de duplicados: pares de equipos de la plantilla que probablemente son el mismo club escrito distinto.

```json
→ 200 OK
{
  "ligaId": "uuid",
  "temporadaNombre": "2026/2027",
  "totalPares": 1,
  "pares": [
    {
      "equipoA": { "id": "uuid", "nombre": "Gimnasia Mendoza", "logoUrl": null },
      "equipoB": { "id": "uuid", "nombre": "Gimnasia y Esgrima Mendoza", "logoUrl": null },
      "razon": "CONTENCION_PALABRAS"
    }
  ]
}
```

- Reglas conservadoras (`CONTENCION_PALABRAS`, `FORMA_JURIDICA`) — jamás marca clubes reales distintos ("Boca Unidos" ≠ "Boca Juniors").
- **Solo informativo**: no fusiona ni elimina nada. UI sugerida: banner ámbar "Revisa estos pares" sobre la plantilla cuando `totalPares > 0`.
- `422` si la liga no existe o no tiene temporadas.

### `POST /api/v1/ligas/{ligaId}/equipos/sincronizar`

Botón ⟳ **"Poblar equipos"** por liga. Reintenta la fuente #6 para esa liga sin re-ejecutar el poblamiento mundial (repara plantillas vacías, ej: scraper caído). Idempotente; roles SUPERADMIN/TIPSTER.

```json
→ 200 OK
{ "creados": 5, "actualizados": 2, "totalEquipos": 30 }
```

Errores: `404`/422 si la liga no existe o la fuente #6 no devuelve equipos (`ApiError.mensaje`).

### Guía UI/UX para la pantalla

```
┌─ Mis países de interés ────────────────────────────────────────┐
│ Colombia                                                       │
│  ┌──────────────────────────────┬──────────┬────────────────┐ │
│  │ Liga Betplay   BORRADOR      │ 👥 30    │ [⟳ Poblar]     │ │
│  │ Primera A      BORRADOR      │ 👥 0  ⚠️ │ [⟳ Poblar]     │ │
│  └──────────────────────────────┴──────────┴────────────────┘ │
│ Argentina                                                      │
│  ┌──────────────────────────────┬──────────┬────────────────┐ │
│  │ Liga Profesional  BORRADOR  │ 👥 30 ✅ │ [⟳ Poblar]     │ │
│  └──────────────────────────────┴──────────┴────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

Recomendaciones:
1. **Responsive**: tabla en desktop → tarjetas apiladas en móvil (una tarjeta por liga: nombre + badge de plantilla + botón).
2. **Badge de plantilla**: verde con check si `totalEquipos > 0`; gris con advertencia si `0` ("plantilla pendiente") — invita a usar el botón.
3. **Feedback del botón**: spinner mientras espera; al recibir respuesta mostrar toast "5 creados · 2 actualizados · 30 en plantilla" y refrescar el badge con `totalEquipos`.
4. **Accesibilidad**: botón con `aria-label="Poblar equipos de {liga}"`; estados disabled mientras corre.
5. Los escudos ya quedan disponibles para el resto del sitio cuando se exponga el detalle de equipos (próximo endpoint planeado).

---
*Fuente de verdad backend: `CatalogoController`, `SincronizarCatalogoUseCase` (CU-10), `SincronizarEquiposLigaUseCase` (CU-16), `LigaController`.*
