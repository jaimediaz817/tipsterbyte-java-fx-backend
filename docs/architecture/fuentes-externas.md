# Fuentes de extracción de datos — tipsterbyte-fx-v2

> **Documento de referencia (FASE 8.5)**. Un proyecto Python aparte se encarga de extraer datos de las web (Soccerway, Flashscore, Wplay) y los expone vía 5 endpoints HTTP. Este proyecto Java (Spring Boot) consume esos endpoints a través de los puertos del dominio (`ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas`) y de nuevos casos de uso (CU-10).
>
> Regla pactada: **no asumir formatos**. Antes de implementar cada adapter se pide al usuario la respuesta real (JSON) del endpoint. Este documento registra QUÉ expone cada recurso, su parámetro y para qué se usa en el modelo.

## Servicio de extracción

- Host base: `http://127.0.0.1:8001`
- Proyecto Python independiente (no forma parte de este repo). Expone recursos con prefijo `ext-`.
- Formato de respuesta: JSON (por confirmar el esquema real de cada recurso).

## Catálogo de recursos

| # | Recurso | Parámetro | Qué expone | Uso en el modelo |
| --- | --- | --- | --- | --- |
| 1 | `/ext-soccerway-countries` | (ninguno) | Todos los países que tienen ligas registradas (Soccerway) | Poblar catálogo de **países** (CU-10) |
| 2 | `/ext-next-matches-wplay-by-league` | `path_to_scrape` (URL de la liga en Wplay) | Cuotas (y cuotas de **doble oportunidad**) de los partidos próximos de la liga | `ProveedorCuotas` (CU-03) |
| 3 | `/ext-position-table-by-league-stable` | `path_to_scrape` (URL de clasificación en Flashscore) | **Tabla de posiciones** de la liga con los **últimos 5 resultados de cada equipo** | `ProveedorPosiciones` (CU-01) |
| 4 | `/ext-calendar-league-by-league-v2` | `path_to_scrape` (URL de resultados en Soccerway) | **Calendario completo** de partidos jugados con estadísticas | `ProveedorCalendario` (CU-02) |
| 5 | `/ext-soccerway-leagues-by-country` | `country_name` (nombre del país) y `limit` | **Ligas existentes/activas en la web** de ese país (no implica ligas activas en nuestro contexto) | Poblar catálogo de **ligas** por país (CU-10) |

## Orden correcto de extracción

```
#1 obtener países → #5 obtener ligas por cada país → luego por liga:
#3 posiciones (con últimos 5 resultados), #4 calendario, #2 cuotas (partidos próximos)
```

1. **Países** (`#1`): sin parámetros; devuelve todos los países con ligas.
2. **Ligas por país** (`#5`): se itera sobre cada país obtenido en `#1`; `country_name` es el nombre del país y `limit` acota el número de ligas.
3. Por cada liga activada (o candidata):
   - **Posiciones** (`#3`): `path_to_scrape` apunta a la clasificación en Flashscore; incluye últimos 5 resultados por equipo.
   - **Calendario** (`#4`): `path_to_scrape` apunta a los resultados en Soccerway.
   - **Cuotas** (`#2`): `path_to_scrape` apunta a la liga en Wplay; trae cuotas y doble oportunidad de los próximos partidos.

## Mapeo tentativo a puertos / casos de uso

| Fuente | Puerto / CU |
| --- | --- |
| `#1` países | CU-10 (nuevo): poblar catálogo de países |
| `#5` ligas por país | CU-10 (nuevo): poblar catálogo de ligas |
| `#2` cuotas Wplay | `ProveedorCuotas` → CU-03 (Sincronizar cuotas) |
| `#3` posiciones + últimos 5 | `ProveedorPosiciones` → CU-01 (Sincronizar posiciones) |
| `#4` calendario + estadísticas | `ProveedorCalendario` → CU-02 (Sincronizar calendario) |

## Esquemas reales confirmados (respuestas del usuario)

### `#1` — `/ext-soccerway-countries` ✅ JSON confirmado

Envoltorio común: `success`, `total` (opcional), `data[]`.

```json
{
  "success": true,
  "total": 176,
  "data": [
    {
      "nombre": "Albania",
      "href": "/albania/",
      "code": "17",
      "iso_alpha2": "AL",
      "continente": "Europa",
      "mapeado": true
    }
  ]
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `nombre` | string | Nombre del país (Ubiquitous Language: `pais`) |
| `href` | string | Ruta relativa en Soccerway |
| `code` | string | Código numérico (id en Soccerway) |
| `iso_alpha2` | string | ISO 3166-1 alfa-2 (ej: `AL`) |
| `continente` | string | `Europa`, `America`, etc. |
| `mapeado` | boolean | Indica si el país ya tiene mapeo en el sistema Python |

### `#5` — `/ext-soccerway-leagues-by-country` ✅ JSON confirmado

Parámetros: `country_name`, `limit`. Respuesta anidada por país.

```json
{
  "success": true,
  "data": [
    {
      "country_name": "España",
      "leagues": [
        {
          "name": "LaLiga EA Sports",
          "type": "League",
          "logo_url": "",
          "api_id": null,
          "url_soccerway": "https://co.soccerway.com/espana/laliga-ea-sports/",
          "nombre_torneo": "LaLiga EA Sports",
          "semestre": "2026/2027",
          "anio": "2026/2027"
        }
      ]
    }
  ]
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `country_name` | string | País (padre; coincide con `#1.nombre`) |
| `name` | string | Nombre de la liga |
| `type` | string | `League` (hay que ver si existen otros valores) |
| `logo_url` | string | Puede venir vacío (`""`) |
| `api_id` | string/null | `null` en el ejemplo; id para mapeo con API-Football |
| `url_soccerway` | string | **URL de Soccerway → candidato a `path_to_scrape` del calendario (`#4`)** |
| `nombre_torneo` | string | Nombre comercial del torneo |
| `semestre` | string | Inconsistente: a veces temporada (`2026/2027`), a veces categoría (`Grupo 1`) → **NO usar como temporada** |
| `anio` | string | Temporada en formato `AAAA/AAAA` → mapea a `Temporada` |

### `#2` — `/ext-next-matches-wplay-by-league` ✅ JSON confirmado

Parámetro: `path_to_scrape` (URL de la liga en Wplay). Devuelve los partidos próximos con sus cuotas.

```json
{
  "success": 200,
  "matches_wplay": [
    {
      "time_match": "14:30",
      "date_match": "15 Ago 2026",
      "date_match_raw": "15 Ago",
      "team_local": "Fluminense RJ",
      "quota_team_local": "2.72",
      "quota_tie": "3.15",
      "team_visiting": "Palmeiras SP",
      "quota_team_visiting": "2.65",
      "double_chance": [
        { "name": "Fluminense RJ/Empate", "name_quota": "1x", "quota": "1.45" },
        { "name": "Fluminense RJ/Palmeiras SP", "name_quota": "12", "quota": "1.30" },
        { "name": "Palmeiras SP/Empate", "name_quota": "2x", "quota": "1.444" }
      ]
    }
  ]
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `success` | **número (200)** | A diferencia de `#1`/`#5` (`success: true`). No asumir envoltorio uniforme. |
| `time_match` | string `"14:30"` | Hora local sin timezone |
| `date_match` | string `"15 Ago 2026"` | **Con año** (corregido). Mes abreviado en español. Fuente de `fechaHora` |
| `date_match_raw` | string `"15 Ago"` | Sin año; **no usar** (redundante) |
| `quota_team_local` | string `"2.72"` | Cuota del local → `Mercado.UNO_X_DOS` |
| `quota_tie` | string `"3.15"` | Cuota del empate → `Mercado.UNO_X_DOS` |
| `quota_team_visiting` | string `"2.65"` | Cuota del visitante → `Mercado.UNO_X_DOS` |
| `double_chance[].name_quota` | string `"1x"/"12"/"2x"` | Selección de doble oportunidad → `Mercado.DOBLE_OPORTUNIDAD` |
| `double_chance[].quota` | string `"1.45"` | Cuota de la doble oportunidad |

**Mapeo**: cada partido produce **6 `CuotaFuente`** (3 de `UNO_X_DOS` + 3 de `DOBLE_OPORTUNIDAD`). Cuotas como `String` → `BigDecimal`. Fecha: `date_match` + `time_match` → `LocalDateTime` (sin timezone, asumida local).

### `#3` — `/ext-position-table-by-league-stable` ✅ JSON confirmado

Parámetro: `path_to_scrape` (URL de la clasificación en Flashscore). Incluye los **últimos 5 resultados por equipo**.

```json
{
  "status_code": 200,
  "tabla_posiciones": [
    {
      "nombre_equipo_full": "Palmeiras",
      "url_logo_equipo": "https://...",
      "posicion": "1",
      "partidos_jugados": "22",
      "partidos_ganados": "14",
      "partidos_empatados": "6",
      "partidos_perdidos": "2",
      "goles_a_favor": "38",
      "goles_en_contra": "16",
      "goles_diferencia": "22",
      "puntos": "48",
      "resultados_ultimos_5_jugados": { "1": 0, "2": 1, "3": -1, "4": 1, "5": 1 }
    }
  ]
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `status_code` | **número (200)** | Envoltorio distinto (`#1`/`#5` usan `success`). |
| `nombre_equipo_full` | string | Nombre del equipo |
| `url_logo_equipo` | string | Logo (no se modela aún) |
| `posicion` / `partidos_*` / `goles_*` / `puntos` | **String** | `"22"`, `"48"` → parsear a `int` |
| `goles_diferencia` | string | Derivado; se puede ignorar (calculable) |
| `resultados_ultimos_5_jugados` | objeto `{1..5: -1\|0\|1}` | **Clave 1 = partido más reciente** (decisión del usuario, orden Flashscore izquierda→derecha). Valores: `1`=G, `0`=E, `-1`=P |

**Decisión de dominio (aprobada)**: los últimos 5 resultados se modelan en **`PosicionTabla`** (fila de clasificación por equipo en la liga). **NO** van en `Partido` (los goles no existen en partidos sin jugar). Clave 1 = más reciente; valores `1/0/-1` mapean a G/E/P.

### `#4` — `/ext-calendar-league-by-league-v2` ✅ JSON confirmado

Parámetro: `path_to_scrape` (URL de resultados en Soccerway). Calendario por jornadas.

```json
{
  "success": true,
  "url": "...",
  "total_partidos": 420,
  "total_partidos_procesados": 420,
  "total_partidos_con_error": 0,
  "partidos_por_jornada": [
    [
      {
        "jornada": "Jornada 4",
        "partido_jugado": true,
        "fecha": "12/08/2026",
        "fecha_original": "11.08.",
        "fecha_iso": "2026-08-12",
        "hora": "19:00",
        "equipo_local": "...",
        "equipo_visitante": "...",
        "goles_local": 0,
        "goles_visitante": 3,
        "url_partido": "...",
        "url_estadisticas": "...",
        "estadisticas": { },
        "partido_procesado_status": true
      }
    ]
  ]
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `success` | boolean `true` | Envoltorio `{success, url, total_*, partidos_por_jornada}` |
| `partidos_por_jornada` | array de arrays | Cada elemento es una jornada; cada jornada es un array de partidos |
| `fecha_iso` | string `"2026-08-12"` | **Fuente de `fechaHora`** (junto a `hora`) |
| `hora` | string `"19:00"` | Hora local sin timezone |
| `equipo_local` / `equipo_visitante` | string | Equipos |
| `partido_jugado`, `goles_local`, `goles_visitante` | boolean / int | Resultado y estado final |
| `estadisticas{}` | objeto | **NO se modela** en esta fase (decisión del usuario) |

**Decisión de alcance (aprobada)**: en FASE 8.5 el adapter **solo crea partidos** → mapea `equipos + fechaHora` a `PartidoFuente`. Goles/estado `FINALIZADO`/estadísticas quedan para un futuro CU de sincronización de resultados (CU-02 hoy crea `PROGRAMADO`).

## Diagnóstico — brecha del modelo de dominio (CU-10)

El modelo actual **no tiene catálogo de países ni de ligas por fuente**:

- `Liga.pais` es un `String` plano; **no existe entidad `Pais`** ni repositorio de países.
- El aggregate `Liga` actual no guarda `url_soccerway` ni `api_id` (claves para sincronizar después por `path_to_scrape`).
- `Temporada` del dominio espera `anioInicio/anioFin`; el campo `anio` (`"2026/2027"`) es el que mapea limpio; `semestre` se descarta.

### Con la info de #1 y #5, ¿es suficiente para poblar?

| Dato requerido | ¿Alcanza? | Fuente |
| --- | --- | --- |
| Países (catálogo) | ✅ Sí | `#1` |
| Ligas por país (catálogo) | ✅ Sí | `#5` |
| Temporada de la liga | ✅ Sí | `#5.anio` |
| URL para calendario (`path_to_scrape` #4) | ✅ Sí | `#5.url_soccerway` |
| Equipos por liga | ⚠️ No | Sale de `#3`/`#4` (posiciones/calendario) |
| URL Flashscore para posiciones (#3) | ⚠️ No | No viene en `#5`; se asocia en activación (CU-04) |
| URL Wplay para cuotas (#2) | ⚠️ No | No viene en `#5`; se asocia en activación (CU-04) |

**Conclusión**: `#1` + `#5` alcanzan para el **catálogo de países y ligas** (CU-10). El detalle deportivo (equipos, posiciones, calendario, cuotas) y las URLs de Flashscore/Wplay se resuelven después, al sincronizar por liga (CU-01/02/03) y en la activación (CU-04).

## Esquemas pendientes de confirmar

- ✅ Todos los esquemas reales están confirmados (`#1` a `#5`). No queda ningún formato por pedir.

## Notas de diseño resueltas en FASE 8.5

- **`#3` "últimos 5 resultados por equipo"** (resuelto): se modela en `PosicionTabla` (fila de clasificación por equipo en la liga). Clave 1 = partido más reciente; `1`=G, `0`=E, `-1`=P. Extiende dominio + DTO `PosicionFuente`.
- **`#2` doble oportunidad** (resuelto): `double_chance[].name_quota` (`1x/12/2x`) → `Mercado.DOBLE_OPORTUNIDAD`; `quota_team_local/tie/visiting` → `Mercado.UNO_X_DOS`. 6 `CuotaFuente` por partido.
- **`#4` alcance** (resuelto): el adapter solo crea partidos (`equipos + fechaHora`). Goles/estado/estadísticas quedan para un futuro CU de resultados.
- **`#1`/`#5` conviven con football-data.org**: aún sin cerrar si las fuentes reales reemplazan o coexisten con las 4 APIs originales (decisión pendiente del usuario).
- **Activación de ligas**: `#3`/`#2` necesitan las URLs de Flashscore/Wplay por liga; la activación (CU-04) debe contemplar cómo se asocian esas URLs a cada liga.
- **Modelado de catálogo (CU-10)**: `Pais` es una nueva entidad del dominio (con su repositorio); `Liga` agrega `urlSoccerway` y `apiId` como datos de la fuente; tabla nueva `paises`.
- **`TipoFuenteExtraccion` es un enum cerrado a propósito** (resuelto tras reporte del frontend): el tipo NO se abre a String libre porque cada tipo mapea a un adapter de extracción específico — un tipo desconocido quedaría registrado pero sin consumidor. Para agregar una fuente nueva el mecanismo de extensión es: 1) añadir el valor al enum, 2) implementar el adapter que lo consume (puerto `ProveedorXxx`), 3) registrar el adapter en `CacheConfig`/wiring. `POST /api/v1/fuentes` valida el enum (400 si no está) y rechaza duplicados por tipo (422).

## Trazabilidad

- Relacionado con FASE 8.5 en `docs/PROYECTO-PLAN.md`.
- Puertos existentes en `application.port`: `ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas`.
- DTOs de fuente existentes en `application.dto`: `PosicionFuente`, `PartidoFuente`, `CuotaFuente`.