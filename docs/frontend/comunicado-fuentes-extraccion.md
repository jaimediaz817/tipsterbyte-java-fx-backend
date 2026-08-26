# Comunicado — Catálogo de Fuentes de Extracción (nuevo campo `url_base_fuente` + edición)

> [QUÉ]: Contrato REST verificado contra el código real para listar, registrar y **editar**
>         las fuentes de extracción, incluyendo el nuevo campo `urlBaseFuente` que permite
>         al formulario mostrar un **enlace directo a la fuente base**.
> [POR QUÉ]: Al activar una liga, el SUPERADMIN necesita construir manualmente la URL
>             específica (país/liga) a partir de la base de la fuente. Hasta ahora esa
>             base vivía en apuntes externos; ahora vive en el catálogo y es editable.
> [RELACIONES]: CU-11 `GestionarFuenteExtraccionUseCase`, `FuenteExtraccionController`,
>               migración V7 (`url_base_fuente`), pantalla "Activar liga".
>               Requiere backend reiniciado con esta versión (bootRun).

---

## 1. Listar fuentes

```
GET /api/v1/fuentes
Authorization: Bearer <token>
Roles: SUPERADMIN | TIPSTER | CLIENTE   (solo lectura; CLIENTE también puede listar)
```

Respuesta **200 OK**:

```json
[
  {
    "id": "uuid",
    "nombre": "Cuotas Wplay",
    "tipo": "ODDS_WPLAY",
    "url": null,
    "activa": true,
    "urlBaseFuente": "http://127.0.0.1:8001"
  }
]
```

| Campo | Tipo | Significado |
|---|---|---|
| `id` | uuid | Identidad de la fuente |
| `nombre` | string | Nombre visible ("Cuotas Wplay", "Tabla de posiciones"...) |
| `tipo` | enum | `STANDINGS \| ODDS_WPLAY \| CALENDAR` (clave natural única) |
| `url` | string\|null | URL específica asociada a una liga; **siempre null** en el listado general (solo tiene sentido en `GET /ligas/{ligaId}/fuentes`) |
| `activa` | bool | Fuente operativa |
| `urlBaseFuente` | string\|null | **NUEVO**: base para construir URLs específicas → usar como href del enlace "ir a la fuente". Puede ser null si nadie la configuró |

## 2. Editar una fuente (NUEVO)

```
PUT /api/v1/fuentes/{tipo}
Authorization: Bearer <token>
Roles: SUPERADMIN | TIPSTER     (CLIENTE → 403)
Body:
{
  "nombre": "Cuotas Wplay",
  "urlBaseFuente": "http://127.0.0.1:8001",
  "activa": true
}
```

- `{tipo}` en el path: clave natural única (`STANDINGS` | `ODDS_WPLAY` | `CALENDAR`). No se
  envía en el body porque no es editable (una fuente por tipo).
- `nombre` obligatorio (400 si falta); `urlBaseFuente` opcional (null la limpia);
  `activa` obligatorio (bool).

Respuesta **200 OK** con la fuente editada (mismo shape del punto 1):

```json
{
  "id": "uuid",
  "nombre": "Cuotas Wplay",
  "tipo": "ODDS_WPLAY",
  "url": null,
  "activa": true,
  "urlBaseFuente": "http://127.0.0.1:8001"
}
```

### Errores

| Status | Situación |
|---|---|
| 400 | `nombre` vacío/ausente, o tipo de path inválido |
| 404 | No existe fuente registrada para ese tipo |
| 403 | Rol sin permiso (CLIENTE intentando editar) |

## 3. Registrar una fuente (existente, ahora acepta url base)

```
POST /api/v1/fuentes
Roles: SUPERADMIN | TIPSTER
Body:
{ "nombre": "...", "tipo": "STANDINGS", "activa": true, "urlBaseFuente": "http://..." }
```

- `urlBaseFuente` opcional. 201 + header Location · 422 si el tipo ya está registrado
  (para editar una existente use el PUT del punto 2, NO el POST).

## 4. Fuentes asociadas a una liga (también expone la base)

```
GET /api/v1/ligas/{ligaId}/fuentes
Roles: SUPERADMIN | TIPSTER
```

Mismo shape del punto 1 pero `url` trae la URL específica de la liga y
`urlBaseFuente` la base de esa fuente → el formulario puede renderizar:

```
URL específica: https://flashscore.com/tabla/xxx   [Abrir]  ← url
Fuente base:    http://127.0.0.1:8001              [Abrir]  ← urlBaseFuente
```

## 5. Notas de implementación (backend)

- Campo persistido: `fuentes_extraccion.url_base_fuente VARCHAR(500)` (migración V7,
  aplicada también en dev con backfill `http://127.0.0.1:8001` para las filas previas).
- La edición reconstruye el aggregate conservando su `id` (el dominio es inmutable):
  cambiar `urlBaseFuente` NO rompe las referencias de `detalle_fuentes_extraccion`.
- Seguridad endurecida en esta versión: escritura sobre `/api/v1/fuentes/**`
  requiere SUPERADMIN o TIPSTER (antes cualquier usuario autenticado podía escribir).
