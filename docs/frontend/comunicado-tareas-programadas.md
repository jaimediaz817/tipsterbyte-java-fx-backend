# 📣 Comunicado frontend — Administración de Tareas programadas

> [QUÉ]: Contrato REST y guía de UI para construir las vistas de administración de tareas programadas (listado, crear/editar, pausar/reanudar, reloj de próxima ejecución e historial de ejecuciones). **Todo el contrato está implementado** (FASE 12.6 + administración completa).
> [POR QUÉ]: El equipo frontend necesita un contrato claro para construir el módulo "Automatización → Tareas programadas" y la acción "Programar" en Geografía sin adivinación. Se verificó contra el código real (controllers, use cases, scheduler, seguridad).
> [RELACIONES]: CU-15 (`GestionarTareasProgramasUseCase`) → `TareaProgramadaController` (`/api/v1/tareas-programadas`) + `CatalogoScheduler` (dispatcher) + `TareaLogRepository` (historial). Roles objetivo `SUPERADMIN`/`TIPSTER`.

---

## 1. Modelo de negocio

- **Una tarea programada** = una fuente de extracción (liga + tipo `CALENDAR`/`ODDS_WPLAY`/`STANDINGS`) **o** el catálogo global (CU-10, sin liga) + una **frecuencia** (expresión cron de 6 segmentos) + un **estado** (activa/pausada).
- Regla de unicidad: **un solo job por (liga, tipo)**; el catálogo global también es único. El backend rechaza duplicados con `422`.
- **Pausar** ≠ eliminar: la tarea queda configurada pero el scheduler no la dispara.
- Cada ejecución genera un **log persistido** (`tarea_log`, PostgreSQL) con `executionId`, resultado, duración y mensaje de error → fuente del historial.

## 2. Estructura de menú / navegación

```
Automatización
└── Tareas programadas              ← pantalla central (ver §3)
    ├── Listado
    ├── Crear / Editar (modal o página)
    └── Detalle (opcional) con historial de ejecuciones

Geografía (detalle de liga)
└── por cada fuente (Posiciones/Calendario/Cuotas): acción "Programar"
    → modal rápido → POST /api/v1/tareas-programadas
```

- **Automatización → Tareas programadas**: gestiona TODAS las tareas (ligas, tipos, catálogo global, cron, estado, próxima ejecución, historial).
- **Geografía → Programar**: creación rápida desde una fuente concreta (precarga `ligaId` + `tipoFuente`; el usuario solo elige frecuencia y la enciende/apaga).

## 3. Vistas propuestas

### 3.1 Listado (tabla/cards)

| Columna | Fuente de datos |
|---|---|
| Fuente | `ligaNombre` + `tipoFuente` humanizado, o "Catálogo global" |
| Frecuencia | `cronExpression` + humanizado (calculado por el frontend, p. ej. lib `cronstrue`) |
| Estado | badge `activa` / `pausada` / `en ejecución` (ver §5) |
| Próxima ejecución | `nextExecution` (reloj en cuenta regresiva, §4) |
| Última ejecución | `lastExecution` + `lastStatus` (éxito/error) |
| Acciones | Pausar/Reanudar, Editar, Ver historial, Eliminar |

Filtros/sort útiles (cliente, hasta que el backend pague): por estado, por tipo de fuente, por liga, orden por prioridad.

### 3.2 Crear / Editar (modal)

```
Nombre/identificación: derivado (liga + tipo) — no es un campo libre en el backend actual
Fuente asociada:
  - selector "Catálogo global" (ligaId y tipoFuente nulos), vía GET /disponibles
  - o liga + tipo (Geografía ya los precarga)
Frecuencia: presets amigables → frecuencia {valor, unidad} (ver §7) + campo "Cron personalizado"
Activa desde el inicio (toggle)
```

### 3.3 Detalle + Historial de ejecuciones

- Cards con la info de la tarea + estado del **último tick del dispatcher**.
- **Historial**: tabla de ejecuciones (timestamp, estado SUCCESS/ERROR, duración ms, errorCode, mensaje) obtenida del endpoint de logs.

## 4. Reloj / próxima ejecución

- El backend **debe** devolver `nextExecution` (ISO-8601) en la lista y el detalle: la calcula con la misma librería del scheduler (`CronExpression`), así el frontend solo hace `nextExecution - now` para el countdown.
- **Comportamiento al llegar a 0**: la tarea se ejecuta de forma asíncrona; mientras corre, el estado visual debe ser **"en ejecución"** (ver §5). Al terminar, el reloj pasa a la siguiente ocurrencia.
- ✅ **Implementado**: `nextExecution` (string ISO-8601) viene en cada `TareaProgramadaResponse`; es `null` si la tarea está pausada o su cron no volverá a disparar.

## 5. Estados visuales de una tarea

| Estado | Condición | Acción principal |
|---|---|---|
| `ACTIVA` | `activa=true` y no corriendo | Pausar (`PUT activa:false`) |
| `PAUSADA` | `activa=false` | Reanudar (`PUT activa:true`) |
| `EJECUTANDOSE` | corriendo en el dispatcher (anti-solapamiento) | Deshabilitar acciones (o mostrar spinner) |
| `ERROR` (última corrida) | último log `status=ERROR` | Ver historial / diagnóstico |

> ✅ **Implementado**: `GET /api/v1/tareas-programadas/ejecucion` devuelve los ids de las tareas corriendo ahora. Usarlo para marcar `EJECUTANDOSE` (polling de pocos segundos) y pausar el reloj; el estado vuelve a `ACTIVA`/`PAUSADA` cuando el id deja de aparecer.

## 6. Contrato REST

Roles objetivo: `SUPERADMIN`/`TIPSTER`. Errores normalizados: `DomainException` → **422**, validación → **400**, no autenticado → **401**, sin permiso → **403** (formato `ApiError` JSON).

### 6.1 Tabla de endpoints

| Método | Ruta | Estado | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/tareas-programadas` | ✅ Implementado | Lista `TareaProgramadaResponse` (incluye `nextExecution` derivada) |
| `GET` | `/api/v1/tareas-programadas/ejecucion` | ✅ Implementado | `Set<UUID>` de tareas **ejecutándose ahora mismo**; `[]` si scheduler desactivado |
| `GET` | `/api/v1/tareas-programadas/disponibles` | ✅ Implementado | Fuentes candidatas para el selector (`FuenteDisponible` con `yaProgramada`) |
| `GET` | `/api/v1/tareas-programadas/{id}/logs` | ✅ Implementado | Historial de ejecuciones de una tarea (`List<TareaLog>`), más reciente primero |
| `GET` | `/api/v1/tareas-programadas/logs?limite=20` | ✅ Implementado | **Últimas (n) ejecuciones de TODAS las tareas** (`List<TareaLog>`), más recientes primero; `limite` en `[1,100]` (default 20) |
| `GET` | `/api/v1/tareas-programadas/{id}` | ✅ Implementado | Detalle `TareaProgramadaResponse`; `422` si no existe |
| `POST` | `/api/v1/tareas-programadas` | ✅ Implementado | Crea con `{ligaId?, tipoFuente?, prioridad, cron?, frecuencia?, activa?}` → `201`; `422` si cron inválido/duplicado |
| `PUT` | `/api/v1/tareas-programadas/{id}` | ✅ Implementado | Pausar/reanudar (`activa`) y editar periodo (`cron`/`frecuencia`) → `200` con la tarea actualizada |
| `DELETE` | `/api/v1/tareas-programadas/{id}` | ✅ Implementado | Elimina; `204`; `422` si no existe |

### 6.2 Shapes actuales (hoy, FASE 12.6 + administración)

`TareaProgramadaResponse` (lo que devuelven GET list, GET /{id}, POST y PUT):
```json
{
  "id": "uuid",
  "ligaId": "uuid | null",
  "ligaNombre": "Premier League | null",
  "tipoFuente": "CALENDAR | ODDS_WPLAY | STANDINGS | null",
  "prioridad": "string",
  "cronExpression": "0 0 2 */8 * *",
  "activa": true,
  "createdAt": "2026-01-01T00:00:00Z",
  "nextExecution": "2026-08-18T02:00:00Z | null"
}
```

`TareaLog` (GET /{id}/logs):
```json
{
  "id": "uuid",
  "tareaProgramadaId": "uuid",
  "executionId": "uuid (correlaciona con el MDC de los logs JSON)",
  "timestamp": "2026-08-17T21:00:00Z",
  "status": "SUCCESS | ERROR",
  "durationMs": 1523,
  "errorCode": "RuntimeException | null",
  "mensaje": "… | null"
}
```

`FuenteDisponible` (GET /disponibles):
```json
{
  "ligaId": "uuid | null",
  "ligaNombre": "Premier League | Catálogo global",
  "tipoFuente": "STANDINGS | null",
  "yaProgramada": false
}
```

### 6.3 Contrato de escritura (POST / PUT)

Body de `POST` (crear):
```json
{
  "ligaId": "uuid | null",
  "tipoFuente": "STANDINGS | null",
  "prioridad": "1",
  "cron": "0 0 3 * * *",          // opcional; si no va, se usa frecuencia o "0 0 * * * *"
  "frecuencia": { "valor": 6, "unidad": "HORAS" },  // opcional; se codifica a cron
  "activa": true
}
```

Body de `PUT /{id}` (pausar/reanudar/editar periodo): acepta el **mismo shape**, todos los campos opcionales; solo actualiza los enviados. Para pausar: `{"activa": false}`. Para cambiar el periodo: `{"frecuencia": {"valor": 8, "unidad": "DIAS"}}` o `{"cron": "0 0 4 * * 1"}`.

> ⚠️ Nota: la respuesta de tarea no trae `lastExecution`/`lastStatus`. Para la columna "Última ejecución" el frontend puede usar el primer elemento de `GET /{id}/logs` (el de esa tarea) o cruzar con `GET /logs?limite=N` (vista global); si no hay logs, la celda va en blanco.

## 7. Frecuencia amigable (el frontend NO escribe crons crudos)

Para que el usuario no escriba crons, el backend acepta `frecuencia: {valor, unidad}` y la codifica internamente a cron de 6 segmentos:

| Unidad | Rangos válidos | Cron generado |
|---|---|---|
| `SEGUNDOS` | 1–59 | `0/N * * * * *` (cada N segundos) |
| `MINUTOS` | 1–59 | `0 0/N * * * *` (cada N minutos) |
| `HORAS` | 1–23 | `0 0 */N * * *` (cada N horas) |
| `DIAS` | 1–30 | `0 0 0 */N * *` (cada N días a medianoche) |

- Si se envían `cron` y `frecuencia` a la vez, **gana `frecuencia`** (es la que el usuario ve/editó); `cron` se ignora. Si no va ninguno, default `0 0 * * * *`.
- Valores fuera de rango o unidad desconocida → `422`.
- Presets concretos que siguen valiendo (pueden enviarse como cron o como frecuencia):

| Preset | Cron equivalente | Frecuencia |
|---|---|---|
| Cada hora | `0 0 * * * *` | `{1, HORAS}` |
| Diario 03:00 | `0 0 3 * * *` | — (requiere cron personalizado para fijar hora) |
| Cada 8 días | `0 0 0 */8 * *` | `{8, DIAS}` |

- **Humanizar la frecuencia para mostrarla** sigue siendo responsabilidad del frontend (lib `cronstrue` sobre `cronExpression`).

## 8. Notas de seguridad / roles ✅

- **Corregido**: `/api/v1/tareas-programadas/**` está restringido a `SUPERADMIN`/`TIPSTER` en `SecurityConfig`. Un `CLIENTE` recibe `403` (`ApiError` JSON). El frontend debe **ocultar el módulo Automatización para rol CLIENTE** igualmente, por UX.
- El resto del módulo requiere token JWT válido (`Authorization: Bearer …`).

## 9. Extras que sugerimos incluir

1. **Historial con diagnóstico**: al ver una tarea en estado ERROR, enlazar al detalle del último log (errorCode + mensaje) y al `executionId` (para buscar en los logs JSON del backend / ELK).
2. **Confirmación antes de eliminar** (destructivo) y de pausar (cambia el comportamiento operativo).
3. **Anti-solapamiento visible**: si el dispatcher omite un tick porque la tarea aún corre, el reloj debe pausarse y avisar "Ejecución en curso".
4. **Deshabilitar acciones mientras corre** la misma tarea para no generar condiciones de carrera con el backend.
5. **Feedback 422 claro**: los errores de dominio llegan con `ApiError`; mostrarlos como alertas (p. ej. "Ya existe una tarea programada para la liga y tipo dados").
6. **Badges por tipo de fuente** (colores por `CALENDAR`/`ODDS_WPLAY`/`STANDINGS`/Catálogo) para leer el listado de un vistazo.
7. **Paginación**: hoy el listado es completo; si crece el volumen, se añadirá paginado (pendiente).
8. **Rate limiting futuro**: el scheduler protege contra solapamiento, pero con muchas tareas simultáneas el scraper puede recibir 429; está previsto como mejora, no bloquea la UI.

---

## Resumen de status codes

| Operación | Éxito | Errores |
|---|---|---|
| `GET /tareas-programadas` | `200` (lista) | — |
| `GET /tareas-programadas/ejecucion` | `200` (Set de ids) | — |
| `GET /tareas-programadas/{id}` | `200` | `422` (no existe) |
| `POST /tareas-programadas` | `201` | `400` / `422` (duplicado, cron inválido) |
| `PUT /tareas-programadas/{id}` | `200` (tarea actualizada) | `422` (no existe, cron inválido) |
| `DELETE /tareas-programadas/{id}` | `204` | `422` (no existe) |
| `GET /tareas-programadas/{id}/logs` | `200` (lista) | `422` (no existe) |
| `GET /tareas-programadas/logs` | `200` (lista, default 20) | `400` (limite fuera de [1,100]) |
| `GET /tareas-programadas/disponibles` | `200` | — |
