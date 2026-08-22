# Comunicado Frontend — Activación manual de liga (CU-04)

> Decisión confirmada: la activación de una liga es **100% manual**. El administrador consulta cada fuente operativa y pega sus 3 URLs en el flujo. No habrá derivación automática para evitar inconsistencias. Este documento alinea el contrato técnico con la experiencia de usuario esperada.
>
> Última actualización: H-01 parcialmente resuelto — `Liga.activar()` ahora transita `PLANIFICADA → ACTIVA` en su temporada vigente. Ver `docs/architecture/hallazgos-arquitectura.md`.

---

## 1. Contrato técnico

### Endpoint

```
POST /api/v1/ligas/{ligaId}/activacion
Content-Type: application/json
Roles: SUPERADMIN / TIPSTER
```

### Request body — las 3 URLs son **obligatorias** (BR-001)

```json
{
  "urlPosiciones": "https://www.flashscore.com/.../standings",
  "urlCalendario": "https://www.soccerway.com/.../results",
  "urlCuotas": "https://www.wplay.co/.../futbol"
}
```

| Campo | Origen | Para qué se usa |
|---|---|---|
| `urlPosiciones` | Fuente operativa #3 — Flashscore standings | Sincronizar tabla de posiciones (CU-01) |
| `urlCalendario` | Fuente operativa #4 — Soccerway | Sincronizar calendario/partidos (CU-02) |
| `urlCuotas` | Fuente operativa #2 — Wplay | Sincronizar cuotas (CU-03) |

Las 3 se persisten como `DetalleFuenteExtraccion(temporadaId, tipo, url, activa=true)` asociadas a la **temporada vigente** de la liga.

### Respuestas

| Código | Cuándo | Body |
|---|---|---|
| `204 No Content` | Activación exitosa — liga pasa a `ACTIVA` y su temporada a `ACTIVA` | — |
| `400 Bad Request` | Body malformado / validación estructural | `ApiError` |
| `404 Not Found` | `ligaId` no existe | `ApiError` |
| `422 Unprocessable Entity` | BR-001 violada (alguna URL vacía/nula) o liga sin temporadas | `ApiError` con `DomainException` |
| `403 Forbidden` | Rol no autorizado | — |

> Idempotente en estado: si la liga ya está `ACTIVA`, el `POST` responde `204` sin efecto (no duplica eventos ni detalles).

---

## 2. Flujo de pantallas y componentes (alineado a UX máxima)

### Ubicación recomendada

```
Sidebar → Geografía → Ligas
  ├── Filtros: País (select), Estado (BORRADOR / ACTIVA)
  └── Tabla de ligas (paginada)
        ├── Badge estado (BORRADOR gris / ACTIVA verde)
        ├── Columna temporada (ej. "2025/2026 · PLANIFICADA → ACTIVA")
        └── Acción por fila:
              BORRADOR  →  [Activar]  (primary)
              ACTIVA    →  [Ver detalle] + [Sincronizar ▾]
```

### Modal "Activar liga" — 3 pasos guiados

**Disparador:** botón `Activar` en fila BORRADOR → abre modal.

**Paso 1 — Contexto (solo lectura, arriba del form):**
- Nombre de la liga, país y temporada vigente (ej. "LaLiga EA Sports — España · 2025/2026").
- Hint: *"Consulta cada fuente y pega su URL. Las 3 son obligatorias."*

**Paso 2 — Formulario (3 campos `type="url"`, obligatorios):**

| Campo | Placeholder | Validación frontend | Ayuda |
|---|---|---|---|
| URL Posiciones | `https://www.flashscore.com/...` | Requerido, debe ser URL válida | Link "¿Dónde la encuentro?" → tooltip con captura de Flashscore standings |
| URL Calendario | `https://www.soccerway.com/...` | Requerido, URL válida | Idem para Soccerway |
| URL Cuotas | `https://www.wplay.co/...` | Requerido, URL válida | Idem para Wplay |

- Validación en cliente: `required` + patrón URL. Botón `Activar` deshabilitado hasta que los 3 sean válidos.
- Cada input con icono de fuente + estado (✓ válido / ✗ inválido).
- Botón secundario `Probar URL` opcional por campo (abre en nueva pestaña para verificación visual rápida, no valida contra backend).

**Paso 3 — Confirmación y feedback:**
- Botón primario `Activar liga` → `POST /{ligaId}/activacion` con spinner.
- `204` → cerrar modal, toast éxito *"Liga activada — temporada 2025/2026 ahora en ejecución"*, refrescar tabla (la fila pasa a ACTIVA, temporada a ACTIVA).
- `422` → mostrar `ApiError.mensaje` inline bajo el form (ej. *"Liga no activable: fuentes no operativas"* o *"La liga no tiene temporadas registradas"*).
- Loading: overlay + `aria-busy`, foco atrapado en modal, `Esc` cancela solo si no está enviando.

---

## 3. Estados y reglas visibles para el usuario

| Estado liga | Estado temporada vigente | Acciones habilitadas |
|---|---|---|
| `BORRADOR` + con temporada | `PLANIFICADA` | `Activar` (modal 3 URLs) |
| `BORRADOR` sin temporada | — | Deshabilitar `Activar` + tooltip *"Puebla el catálogo primero"* |
| `ACTIVA` | `ACTIVA` | `Sincronizar posiciones / calendario / cuotas` + `Ver detalle` |
| `ACTIVA` | `FINALIZADA` | Solo lectura / historial (futuro) |

---

## 4. Checklist de implementación frontend

- [ ] Tabla de ligas con filtro por país y estado (`GET /api/v1/ligas?estado=&pais=` ya existe; `GET /api/v1/ligas/{id}` para detalle).
- [ ] Modal de activación con 3 inputs URL + validación + spinner + toasts.
- [ ] Manejo de `204` (éxito) vs `422` (BR-001) con mensajes del backend.
- [ ] Refresco optimista de la fila tras activar (sin recargar toda la tabla).
- [ ] Accesibilidad: `role="dialog"`, `aria-modal`, foco inicial en primer input, `aria-label` por campo, contraste de badges.
- [ ] Responsive: tabla → cards apiladas en móvil (badge + botón `Activar` full-width).

---

## 5. Qué NO hacer (por decisión explícita)

- **No derivar URLs automáticamente** desde `urlSoccerway` del catálogo. Toda URL se pega manualmente para evitar incoherencias entre fuentes.
- **No exponer transición de temporada separada** por ahora (`POST /temporadas/{id}/activar` queda para fase futura si se necesita control fino; hoy ocurre implícitamente al activar la liga).

---

## 6. Estado actual vs. preparación para dinamizar (sin romper el flujo)

**Flujo actual — preservado y sin daño:**

`Geografía → seleccionar país → seleccionar liga (BORRADOR con temporada) → modal con 3 inputs fijos → POST /{ligaId}/activacion`

Este flujo **sigue intacto** y es el que debe implementar el frontend ahora. El backend valida exactamente 3 URLs y responde `204` como hasta ahora.

**Por qué hoy parece "hardcodeado" — y lo está, a propósito en esta capa:**

| Capa | Punto hardcodeado a 3 | Ya es dinámico por debajo |
|---|---|---|
| `TipoFuenteExtraccion` | Enum con `STANDINGS`, `CALENDAR`, `ODDS_WPLAY` | — |
| `ActivarLigaRequest` / `ActivarLigaComando` | 3 campos explícitos (`urlPosiciones`, `urlCalendario`, `urlCuotas`) | `FuenteExtraccion` + `fuentes_extraccion` ya es catálogo N-fuentes (CU-11 permite registrar nuevas) |
| `ActivarLigaUseCase` | Itera 3 tipos fijos para BR-001 | `DetalleFuenteExtraccion(temporadaId, tipo, url)` ya soporta N tipos por temporada |
| Frontend (estimado) | 3 inputs fijos en el modal | — |

**Cómo queda preparado para dinamizar (sin implementar aún):**

Cuando se decida soportar N fuentes operativas, el cambio es **compatible y acotado** — no requiere migración de datos:

1.  Backend: `ActivarLigaRequest` pasará de 3 campos a `List<{tipo, url}>` (o `Map<TipoFuenteExtraccion, String>`), `ActivarLigaUseCase` iterará el catálogo activo de `FuenteExtraccion` en lugar de 3 constantes, y BR-001 se evaluará como "todas las fuentes marcadas como requeridas tienen URL".
2.  Frontend: en lugar de 3 inputs fijos, hará `GET /api/v1/fuentes-extraccion` (CU-11, ya existe) y **renderizará N inputs dinámicamente** según lo que devuelva el catálogo — el flujo `país → liga → formulario` no cambia, solo el número de campos que pinta.
3.  Compatibilidad: se mantendrá el contrato actual de 3 campos como alias durante una versión para no romper el formulario ya implementado.

> **Acción ahora:** implementar el modal con 3 inputs fijos tal cual está documentado arriba. La dinamización queda diseñada y sin deuda de datos; se activa cuando el negocio lo pida añadiendo un tipo al enum/catálogo y cambiando el DTO a lista.

---

*Fuente de verdad backend: `LigaController.activarLiga` → `ActivarLigaUseCase` → `Liga.activar()` (BR-001 + `Temporada.activar()`), `ActivarLigaRequest`.*
