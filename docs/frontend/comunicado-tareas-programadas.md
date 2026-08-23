# 📣 Comunicado frontend — Administración de Tareas Programadas (CU-15)

> [QUÉ]: Contrato técnico completo del panel **Automatización → Tareas programadas**: endpoints disponibles, cómo consumirlos, estructura de navegación recomendada (sidebar) y expectativas de experiencia de usuario.
> [POR QUÉ]: Contextualizar al equipo frontend el estado ACTUAL del sistema tras los últimos refactors (temporadas como centro del modelo + fuente #6 de equipos), para que la UI se implemente funcional a la primera.
> [RELACIONES]: CU-15 → `TareaProgramadaController`; scheduler → `CatalogoScheduler`; flujos que despacha: CU-10 (poblamiento geográfico) y CU-01/02/03 (fuentes operativas).

---

## 1. Contexto técnico — qué cambió respecto al plan original

**Refactor importante**: las tareas NO se asocian a un "detalle de fuente de extracción" (como decía el plan inicial). La implementación final asocia cada tarea directamente a:

| Concepto | Identificación | Qué ejecuta cuando dispara |
|---|---|---|
| **Tarea global** ⭐ | SIN `ligaId` y SIN `tipoFuente` | **Poblamiento geográfico COMPLETO**: países (#1, cache) → ligas por país (#5, respetando `maxLigasPorPais`) → temporada de catálogo por liga → **equipos con escudos (#6, solo países de interés, cache)**. Es exactamente el mismo flujo del botón manual "Poblar catálogo". Solo puede existir UNA |
| **Tarea por fuente de liga** | `ligaId` + `tipoFuente` ∈ {STANDINGS, CALENDAR, ODDS_WPLAY, EQUIPOS} | STANDINGS/CALENDAR/ODDS_WPLAY: sincronización operativa (requiere liga ACTIVA con URL asociada). **EQUIPOS** (nuevo): refresca la plantilla con escudos desde la fuente #6 — NO requiere URL asociada. Una sola tarea por combinación |

**Otros ajustes relevantes**:
- Cada tarea tiene `prioridad` (String libre, ej: `"ALTA"`, `"NORMAL"`) que define el orden de evaluación del dispatcher.
- El scheduler corre **solo si `scheduler.enabled=true`** (default en dev/prod; apagado en tests).
- Dispatcher evalúa cada minuto (`scheduler.interval`, default 60000 ms): una cron dispara en el minuto que le corresponde.
- Anti-solapamiento: si una corrida sigue activa en la siguiente cita, esa cita se omite.
- Cada ejecución queda registrada en `tarea_log` (estado SUCCESS/ERROR, duración, executionId).

> ✅ **Actualizado**: la fuente #6 (equipos) YA es programable por liga — tipo `EQUIPOS` disponible en `POST` y en `GET /disponibles` (sin requerir URL).

## 2. Estructura de navegación recomendada (sidebar)

```
Sidebar
└── Automatización
    ├── Tareas programadas        ← PANEL CENTRAL (esta UI)
    └── Historial de ejecuciones  ← (opcional v2: vista global de logs)
Geografía
├── Países de interés             ← aquí vive también "Ligas de mis países"
│     └── por liga: acción "⟳ Poblar equipos" (botón manual, ya comunicado)
└── [detalle de liga]             ← acción "Programar" sobre cada fuente activa
```

Dos puertas de creación (ambas válidas):
1. **Panel central**: botón "Nueva tarea" → modal con selector de fuente (usa `GET /disponibles`: primera opción "Catálogo global", luego cada liga+fuente activa marcando las ya programadas).
2. **Desde Geografía**: en el detalle de una liga, cada fuente operativa muestra acción "Programar" → mismo modal pre-cargado con `ligaId` + `tipoFuente`.

## 3. Endpoints (roles SUPERADMIN/TIPSTER — 403 para CLIENTE)

Base: `/api/v1/tareas-programadas`

### 3.1 `GET /api/v1/tareas-programadas`
Lista completa ordenada por prioridad. Shape por ítem:

```json
{
  "id": "uuid",
  "ligaId": null,
  "ligaNombre": null,
  "tipoFuente": null,
  "prioridad": "NORMAL",
  "cronExpression": "0 0 3 * * *",
  "activa": true,
  "createdAt": "2026-08-21T12:00:00Z",
  "nextExecution": "2026-08-22T03:00:00-05:00"
}
```

Reglas de interpretación:
- `ligaId == null && tipoFuente == null` ⇒ **tarea global de poblamiento geográfico** → píntala como fila destacada ("Poblamiento geográfico").
- `nextExecution: null` ⇒ pausada o cron sin próximas ejecuciones (mostrar "—").

### 3.2 `GET /disponibles`
Candidatas para el modal de creación (+flag de duplicadas):

```json
[
  { "ligaId": null,   "ligaNombre": "Catálogo global",  "tipoFuente": null,        "yaProgramada": false },
  { "ligaId": "uuid", "ligaNombre": "Premier League",   "tipoFuente": "STANDINGS", "yaProgramada": true }
]
```

`yaProgramada: true` ⇒ deshabilita esa opción en el selector.

### 3.3 `POST /api/v1/tareas-programadas` → `201` + tarea creada

```json
// A) POBLAMIENTO GEOGRÁFICO PROGRAMADO (tarea global — sin liga ni tipo):
{ "cron": "0 0 3 * * *", "activa": true }

// B) Por fuente de liga (ej: cuotas Wplay cada hora):
{
  "ligaId": "uuid-liga",
  "tipoFuente": "ODDS_WPLAY",
  "prioridad": "ALTA",
  "frecuencia": { "valor": 1, "unidad": "HORAS" },
  "activa": true
}
```

- Enviar **cron crudo O frecuencia amigable** (frecuencia gana si vienen ambos). Sin ninguno: default diario 00:00.
- Frecuencia: `{ valor, unidad }` con unidad ∈ `SEGUNDOS | MINUTOS | HORAS | DIAS`; rangos válidos: segundos/minutos 1–59, horas 1–23, días 1–30 (validar también en cliente).
- Errores: `422` duplicado (global ya existe, o liga+tipo ya existe), cron inválido, liga/fuente inexistente o inactiva. `400` validación estructural.

### 3.4 `PUT /api/v1/tareas-programadas/{id}` → `200` + tarea actualizada

```json
{ "activa": false }                                   // pausar
{ "frecuencia": { "valor": 6, "unidad": "HORAS" } }   // cambiar frecuencia
```

Parcial: solo aplica campos presentes. Útil para el toggle pausar/reanudar de la tabla.

### 3.5 `DELETE /api/v1/tareas-programadas/{id}` → `204` (`422` si no existe)

### 3.6 Monitoreo

| Endpoint | Devuelve |
|---|---|
| `GET /api/v1/tareas-programadas/ejecucion` | `[uuid…]` tareas ejecutándose AHORA (badge en vivo) |
| `GET /api/v1/tareas-programadas/{id}/logs` | historial de ESA tarea |
| `GET /api/v1/tareas-programadas/logs?limite=20` | últimas N ejecuciones globales (límite 1–100) |

Shape `TareaLog`: `{ id, tareaProgramadaId, executionId, fechaEjecucion, estado: "SUCCESS"|"ERROR", duracionMs, tipoError, mensaje }`.

## 4. Experiencia de usuario esperada

### Panel central (Automatización → Tareas programadas)

```
┌───────────────────────────────────────────────────────────────────────────────┐
│ Tareas programadas                                    [+ Nueva tarea]         │
├───────────────────────────────────────────────────────────────────────────────┤
│ ⭐ Poblamiento geográfico      0 0 3 * * *   ▶ Activa   Próx: mañana 03:00    │
│    [Editar] [⏸ Pausar] [🗑]                          badge 🟢 Ejecutando…    │
│ Cuotas · Premier League        0 0 * * * *   ⏸ Pausada  Próx: —               │
│ Posiciones · Liga Betplay      0 0 6 */1 * * ▶ Activa   Próx: hoy 06:00       │
├───────────────────────────────────────────────────────────────────────────────┤
│ Últimas ejecuciones                                                           │
│ ✅ Poblamiento geográfico · hace 2 h · 4 min 12 s                              │
│ ❌ Cuotas · Premier League · ayer 23:00 · TimeoutError                        │
└───────────────────────────────────────────────────────────────────────────────┘
```

Checklist UX:

1. **Fila destacada para la tarea global**: icono 🌎/⭐, label "Poblamiento geográfico" (NO mostrar "null"). Tooltip: "Países → ligas → torneos → equipos".
2. **Columna próxima ejecución** con cuenta regresiva derivada de `nextExecution` (refrescar cada minuto); "—" si null.
3. **Badge "Ejecutando…"**: polling de `GET /ejecucion` cada 15–30 s con panel abierto; al desaparecer el id, refrescar logs de esa tarea.
4. **Toggle activa/pausada inline** (switch) → `PUT {activa}` → actualizar fila con la respuesta (sin re-GET).
5. **Modal de creación/edición**:
   - Paso 1: selector de fuente (`GET /disponibles`) con búsqueda; opciones `yaProgramada` deshabilitadas con tooltip "Ya tiene tarea".
   - Paso 2: frecuencia con presets amigables (cada N horas/días…) + pestaña "avanzado" para cron crudo con validación client-side (6 segmentos).
   - Toggle activa + prioridad opcional (text input corto; default "NORMAL").
6. **Estados vacíos**: sin tareas → ilustración + CTA "Crear tu primera tarea" sugiriendo la tarea global.
7. **Errores accionables**: 422 duplicado → mensaje "Ya existe una tarea para esta fuente"; cron inválido → marcar campo.
8. **Responsive**: tabla → tarjetas apiladas en móvil (nombre de tarea grande, chips de estado, acciones en menú ⋮). Modales full-screen en móvil.
9. **Accesibilidad**: switches con `role="switch"` + labels; iconos con `aria-label`; foco visible en modales.
10. **Zona horaria**: los crons se evalúan en hora del SERVIDOR — indicarlo en el helper del modal ("La programación usa la hora del servidor, UTC-5").

### Desde Geografía (creación contextual)

- Acción "Programar" por fuente activa → modal con liga+tipo precargados (ocultar paso 1).
- Deshabilitar si `yaProgramada` (consultar `/disponibles` al abrir).

## 5. Resumen de status codes

| Operación | Éxito | Errores |
|---|---|---|
| GET lista/detalle/disponibles/ejecución/logs | `200` | `403` rol |
| POST | `201` + tarea | `422` duplicado/cron/fuente · `400` |
| PUT | `200` + tarea | `422` no existe/cron |
| DELETE | `204` | `422` no existe |

---
*Fuente de verdad backend: `TareaProgramadaController`, `GestionarTareasProgramasUseCase` (CU-15), `CatalogoScheduler` (`scheduler.enabled=true`, tick 60 s), `docs/architecture/fuentes-externas.md`.*
