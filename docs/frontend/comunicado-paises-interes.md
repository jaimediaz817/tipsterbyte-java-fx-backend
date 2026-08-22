# 📣 Comunicado frontend — Contrato de Países de interés (CU-14)

> [QUÉ]: Aclaraciones del contrato REST de CU-14 (países de interés) resueltas con el equipo frontend: shapes, status codes, semántica de prioridad y el límite `maxLigasPorPais`. Fuente de verdad para la UI de "Poblamiento → Países de interés".
> [POR QUÉ]: El equipo frontend pidió confirmación de los detalles de contrato antes de implementar el selector y la lista ordenable; se verificaron contra el código real para evitar ambigüedad.
> [RELACIONES]: CU-14 → `GestionarPaisesInteresUseCase` → `PaisInteresController` (GET/POST/PUT/DELETE `/api/v1/paises-interes` + GET `/api/v1/paises/disponibles`). El límite lo consume CU-10 al poblar.

---

## 0. ⭐ `maxLigasPorPais` — límite de ligas a extraer por país (NUEVO)

**Endpoint responsable**: `PUT /api/v1/paises-interes` (reemplazo en bloque) y también `POST /api/v1/paises-interes` (alta individual). Es un campo **opcional por ítem**:

| Campo | Tipo | Validación | Semántica |
|---|---|---|---|
| `maxLigasPorPais` | `Integer` o `null` | `>= 1` si viene (si no → **400**) | `null` = sin límite; `n` = extraer máximo n ligas de ese país |

- **Shape completo de cada ítem** (request): `{ isoAlpha2, nombre, maxLigasPorPais }`.
- **Shape de respuesta** (GET lista y POST): `{ isoAlpha2, nombre, prioridad, maxLigasPorPais }`.
- En el JSON de respuesta el campo siempre está presente (`null` explícito cuando no hay límite).
- **Upsert**: si el país ya estaba registrado, el POST/PUT actualiza nombre y límite **conservando su prioridad**.
- **Quién lo consume**: CU-10 (`SincronizarCatalogoUseCase`) al poblar — procesa los países de interés primero y para cada uno extrae como máximo `maxLigasPorPais` ligas (el límite viaja también a la fuente #5 vía param `limit`). Los países SIN preferencia no tienen tope.
- Ejemplo de guardado en bloque ("guardar preferencias"):

```json
PUT /api/v1/paises-interes
[
  { "isoAlpha2": "CO", "nombre": "Colombia", "maxLigasPorPais": 5 },
  { "isoAlpha2": "ES", "nombre": "España",   "maxLigasPorPais": null }
]
→ 204 No Content
```

---

## 1. `GET /api/v1/paises/disponibles`

- Shape: `{ isoAlpha2, nombre, continente, code, href, mapeado }`.
- **No incluye `id`**: los países disponibles aún no están persistidos; por eso el shape difiere de `PaisResponse` (que sí lleva `id`).
- **Los ~176 países en una sola respuesta, sin paginación**, ordenados por `nombre` (case-insensitive).
- El campo `continente` está disponible para los **badges del selector**.
- Rol: `SUPERADMIN`/`TIPSTER`.

## 2. `POST /api/v1/paises-interes` (upsert)

- Responde **`201 Created` siempre** (Location: `/api/v1/paises-interes`), sea país nuevo o ya existente.
- **Devuelve el país creado en el body**: `{ isoAlpha2, nombre, prioridad, maxLigasPorPais }` — la `prioridad` es la asignada por el backend (nueva = siguiente libre; existente = la que conserva). Usar el body para actualizar el UI sin recalcular el orden.
- **Semántica de prioridad**:
  - **País ya registrado + POST** → *upsert*: se actualiza nombre y `maxLigasPorPais` **conservando su prioridad actual** (no se mueve al final).
  - **País desmarcado (`DELETE`) + `POST` de nuevo** → se agrega **al final** de la lista (prioridad = máxima + 1).
  - Regla: *ya marcado = conserva su lugar; re-marcar tras desmarcarlo = va al final*.
- Errores: `400` si falta `isoAlpha2`/`nombre` o si `maxLigasPorPais < 1`; `422` si el `isoAlpha2` no existe en la fuente #1.

## 3. `PUT /api/v1/paises-interes` (reemplazo en bloque)

- Responde **`204 No Content`** (sin body).
- El orden del arreglo enviado define la prioridad (1..n); elimina los países que no vengan en la lista.
- Cada ítem puede llevar `maxLigasPorPais` (ver sección 0). **Enviar la lista COMPLETA**: los países ausentes se eliminan y sus límites con ellos.
- Para refrescar la UI tras reordenar: aplicar localmente la lista enviada (prioridad = posición) o hacer un `GET` para la respuesta canónica.
- Errores: `400` si algún ítem viola validaciones; `422` si algún `isoAlpha2` no existe en la fuente (validación todo-o-nada).

## 4. `GET /api/v1/paises-interes`

- Shape: `{ isoAlpha2, nombre, prioridad, maxLigasPorPais }`.
- La **`prioridad` viene explícita** en el JSON (entero, 1 = primero), no solo implícita en el orden del arreglo. Usar para el orden visual y para construir el arreglo del `PUT`.
- `maxLigasPorPais` permite pre-cargar el input de límite de cada país al abrir "Mis preferidos".

## 5. `DELETE /api/v1/paises-interes/{isoAlpha2}`

- **`204 No Content`**, solo path variable, **sin body ni nombre**.
- `422` si el país no está registrado.

---

## Resumen de status codes

| Operación | Éxito | Errores |
|---|---|---|
| `GET /paises/disponibles` | `200` (lista completa) | — |
| `GET /paises-interes` | `200` (lista por prioridad, incluye `maxLigasPorPais`) | — |
| `POST /paises-interes` | `201` (siempre, upsert) | `400` / `422` |
| `PUT /paises-interes` | `204` | `400` / `422` |
| `DELETE /paises-interes/{isoAlpha2}` | `204` | `422` |