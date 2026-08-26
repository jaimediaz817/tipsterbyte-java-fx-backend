# Respuesta Frontend — Confirmaciones HU-14/HU-15

> [QUÉ]: Documento de trazabilidad con las respuestas a las dudas técnicas del team frontend
>         sobre los contratos de HU-14 (tareas programadas v2) y HU-15 (cuotas próximas + volatilidad).
> [POR QUÉ]: Los chicos del frontend confirmaron que la vista liga-céntrica ya existe en
>             /operaciones/programacion y van a añadir sin reescribir. Respondieron las 5
>             preguntas del comunicado y plantearon 4 dudas técnicas que bloquean su F0.
> [RELACIONES]: comunicado-hu14-hu15-tareas-cuotas.md · historias-de-usuario.md HU-14/HU-15

---

## 1. Confirmaciones del frontend (a favor)

| # | Pregunta del backend | Respuesta del frontend |
|---|---------------------|----------------------|
| 1 | ¿Jerarquía liga → filas por fuente? | ✅ SÍ. **Ya existe** `tareasPorLiga` en `/operaciones/programacion`. No reescriben — solo añaden. Piden no romper la unicidad de la card de poblamiento (`ligaId=null`). |
| 2 | ¿`proximaEjecucion` calculada por backend? | ✅ SÍ. Actualmente hacen `describirCron()` con regex frágiles. Prefieren jubilar esa lógica y consumir el cálculo del servidor. |
| 3 | Ventana default 24h en Cuotas Próximas | ✅ OK. Con selector 6/24/72 para HU-15. |
| 4 | Polling vs refresh manual | ✅ Refresh manual para v1 (botón + refresh tras acciones). Polling solo si la operación lo exige (aprendieron con HU-12). |
| 5 | ¿`/disponibles` alcanza para crear? | ✅ Sí, pero piden **flag `fuenteActiva`** en `GET /tareas-programadas` para pintar badge "fuente inactiva" sin heurísticas de cliente. |

---

## 2. Respuestas del backend a las dudas técnicas (A-D)

### A. Shape de primerDisparo

| Pregunta | Respuesta |
|----------|-----------|
| ¿GET /tareas-programadas devuelve `primerDisparo`? | **Sí**, ya está en el diseño. Se devuelve como `string \| null` en el response JSON. |
| ¿PUT acepta `null` para limpiarlo? | **Sí**, enviar `null` en `primerDisparo` elimina el delay y la tarea retoma solo el cron. |
| Timezone esperada | `America/Bogota` (`-05:00`), sin DST. El frontend debe enviar ISO-8601 con offset explícito: `2026-08-27T06:00:00-05:00`. El backend almacena y valida con esa zona. |

**Shape ejemplo en GET:**
```json
{
  "id": "...",
  "ligaId": "...",
  "tipoFuente": "ODDS_WPLAY",
  "frecuencia": { "valor": 1, "unidad": "HORAS" },
  "primerDisparo": "2026-08-27T18:00:00-05:00",
  "activa": true,
  "prioridad": 1,
  "createdAt": "2026-08-26T10:00:00Z"
}
```

### B. PUT masivo `/liga/{ligaId}/estado`

| Pregunta | Respuesta |
|----------|-----------|
| ¿Shape 200 confirmado? | **Sí**: `{ "ligaId": "...", "activa": false, "tareas": [{"tipoFuente": "ODDS_WPLAY", "activa": false}, ...] }` |
| ¿Actualiza `nextExecution`? | No calcula `nextExecution` en el response del PUT masivo. Ese campo se expondrá en un futuro endpoint dedicado si lo piden. |
| ¿404 solo sin tareas registradas? | **Sí**, 404 solo si la liga no tiene ninguna tarea en `tareas_programadas`. Si tiene tareas pero todas ya están pausadas, retorna 200 con `activa: false` en todas. |
| Rol requerido | **SUPERADMIN** (misma regla que crear/editar). El frontend puede ocultar el botón de pausa masiva para usuarios sin ese rol. |

### C. SUCCESS con 0 elementos — campo explícito

| Pregunta | Respuesta |
|----------|-----------|
| ¿Campo `elementosProcesados`? | **Sí**, se añade a la respuesta de `GET /{id}/logs` como campo numérico. Ejemplo: `{"tareaLogId": "...", "resultado": "SUCCESS", "elementosProcesados": 0, ...}`. El frontend puede usar este campo directamente para el chip "sin datos aún" sin parsear texto. |

**Shape del log con el nuevo campo:**
```json
{
  "id": "...",
  "tareaProgramadaId": "...",
  "resultado": "SUCCESS",
  "duracionMs": 12340,
  "elementosProcesados": 0,
  "mensaje": "Wplay: 0 matches encontrados para liga X",
  "ejecutadaEn": "2026-08-27T18:05:00Z"
}
```

### D. Disponibilidad y despliegue

| Pregunta | Respuesta |
|----------|-----------|
| ¿Cuándo estarán PUT masivo + primerDisparo? | Con la implementación de HU-14 (backend). F1 de DTOs del frontend puede arrancar **en paralelo** sin esperar. |
| ¿Qué necesita HU-14 completa? | Backend + tests + migración V8 (`primer_disparo`), V9 (`equipos_alias`), V10 (`cuota_historial`). El PUT masivo es una extensión ligera de CU-15 existente. |
| Endpoint adicional propuesto | `GET /tareas-programadas` con flag `fuenteActiva` por tarea (reutiliza la query existente + JOIN con `detalle_fuentes_extraccion`). |

---

## 3. Acciones acordadas

| Acción | Responsable | Estado |
|--------|-------------|--------|
| F1 de DTOs frontend (formularios + estandarización) | Frontend | 🟡 Puede arrancar en paralelo |
| Implementación HU-14 backend (primerDisparo, pausa masiva, equipos_alias, historial) | Backend | 🔵 Pendiente |
| Campo `elementosProcesados` en logs | Backend | 🔵 Pendiente (se resuelve en HU-14) |
| Flag `fuenteActiva` en GET /tareas-programadas | Backend | 🔵 Pendiente (se resuelve en HU-14) |
| Endpoint `proximaEjecucion` | Backend | 🟡 Futuro (no bloquea HU-14) |
| Comunicado final de shapes confirmados | Backend + Frontend | ⚪ Este doc sirve como base |

---

## 4. Pregunta pendiente del frontend

> *"¿Les vale así o preferís que lo dejemos en un doc `docs/frontend/respuesta-HU14-HU15.md` para trazabilidad?"*

**Respuesta del backend**: Este archivo (`respuesta-HU14-HU15.md`) es el doc de trazabilidad que confirma los contratos. Se mantiene actualizado conforme avance HU-14.
