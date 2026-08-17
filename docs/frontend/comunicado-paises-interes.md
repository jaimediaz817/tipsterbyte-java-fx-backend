# 📣 Comunicado frontend — Contrato de Países de interés (CU-14)

> [QUÉ]: Aclaraciones del contrato REST de CU-14 (países de interés) resueltas con el equipo frontend: shapes, status codes y semántica de prioridad. Fuente de verdad para la UI de "Poblamiento → Países de interés".
> [POR QUÉ]: El equipo frontend pidió confirmación de los detalles de contrato antes de implementar el selector y la lista ordenable; se verificaron contra el código real para evitar ambigüedad.
> [RELACIONES]: CU-14 → `GestionarPaisesInteresUseCase` → `PaisInteresController` (GET/POST/PUT/DELETE `/api/v1/paises-interes` + GET `/api/v1/paises/disponibles`).

---

## 1. `GET /api/v1/paises/disponibles`

- Shape: `{ isoAlpha2, nombre, continente, code, href, mapeado }`.
- **No incluye `id`**: los países disponibles aún no están persistidos; por eso el shape difiere de `PaisResponse` (que sí lleva `id`).
- **Los ~176 países en una sola respuesta, sin paginación**, ordenados por `nombre` (case-insensitive).
- El campo `continente` está disponible para los **badges del selector**.
- Rol: `SUPERADMIN`/`TIPSTER`.

## 2. `POST /api/v1/paises-interes` (upsert)

- Responde **`201 Created` siempre** (Location: `/api/v1/paises-interes`), sea país nuevo o ya existente.
- **Semántica de prioridad** (aclara la aparente contradicción del comunicado inicial):
  - **País ya registrado + POST** → *upsert*: se actualiza el nombre si cambió y **conserva su prioridad actual** (no se mueve al final).
  - **País desmarcado (`DELETE`) + `POST` de nuevo** → se agrega **al final** de la lista (prioridad = máxima + 1).
  - Regla: *ya marcado = conserva su lugar; re-marcar tras desmarcarlo = va al final*.
- Errores: `400` si falta `isoAlpha2`/`nombre`; `422` si el `isoAlpha2` no existe en la fuente #1.

## 3. `PUT /api/v1/paises-interes` (reemplazo en bloque)

- Responde **`204 No Content`** (sin body).
- El orden del arreglo enviado define la prioridad (1..n); elimina los países que no vengan en la lista.
- Para refrescar la UI tras reordenar: aplicar localmente la lista enviada (prioridad = posición) o hacer un `GET` para la respuesta canónica.
- Errores: `422` si algún `isoAlpha2` no existe en la fuente (validación todo-o-nada).

## 4. `GET /api/v1/paises-interes`

- Shape: `{ isoAlpha2, nombre, prioridad }`.
- La **`prioridad` viene explícita** en el JSON (entero, 1 = primero), no solo implícita en el orden del arreglo. Usar para el orden visual y para construir el arreglo del `PUT`.

## 5. `DELETE /api/v1/paises-interes/{isoAlpha2}`

- **`204 No Content`**, solo path variable, **sin body ni nombre**.
- `422` si el país no está registrado.

---

## Resumen de status codes

| Operación | Éxito | Errores |
|---|---|---|
| `GET /paises/disponibles` | `200` (lista completa) | — |
| `GET /paises-interes` | `200` (lista por prioridad) | — |
| `POST /paises-interes` | `201` (siempre, upsert) | `400` / `422` |
| `PUT /paises-interes` | `204` | `422` |
| `DELETE /paises-interes/{isoAlpha2}` | `204` | `422` |