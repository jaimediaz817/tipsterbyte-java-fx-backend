# 📖 Manual de Migraciones — Flyway (H-05)

> [QUÉ]: Guía práctica para crear, ejecutar y validar migraciones de esquema en el ecosistema TipsterByte FX. Incluye el flujo para entidades/columnas nuevas, qué hacer con registros existentes y cómo llevar datos de dev a producción sin pérdidas.
> [POR QUÉ]: Desde H-05 el esquema NO lo modifica Hibernate (`ddl-auto=validate`): la única fuente de verdad son los `.sql` versionados de esta carpeta. Este manual es el paso a paso para trabajar con ellos sin romper nada.
> [RELACIONES]: `db/migration/V1__baseline.sql` · `application.properties` (`spring.flyway.*`, `ddl-auto=validate`) · hallazgo H-05 en `docs/architecture/hallazgos-arquitectura.md`.

---

## 0. Cómo funciona (30 segundos)

```
Arranque de la app (bootRun o tests)
  └─ Flyway mira la tabla flyway_schema_history
       ├─ BD vacía (Testcontainers/prod nueva) → ejecuta TODAS las migraciones en orden
       └─ BD existente (dev :5434)             → ejecuta solo las pendientes
  └─ Hibernate (ddl-auto=validate) verifica que tus Entities coinciden con el esquema
       └─ Si algo no coincide → arranque FALLA con error explícito (nunca silencioso)
```

- **BD dev (:5434)** ya fue "baselinada": Flyway marcó `V1` como aplicada **sin tocar ni una fila**. Las siguientes migraciones se aplican normalmente encima.
- La tabla de control es `flyway_schema_history` (versión, descripción, checksum, fecha). Consúltala en DBeaver como cualquier tabla.

---

## 0b. Comandos disponibles (Gradle)

| Comando | Qué hace |
|---|---|
| `./gradlew nuevaMigracion -Pdescripcion=mi_cambio` | Crea el siguiente `V(n+1)__mi_cambio.sql` con numeración correlativa + header |
| `./gradlew migrar` | Aplica en la BD dev las migraciones pendientes (sin arrancar la app) |
| `./gradlew infoMigraciones` | Estado de cada migración en dev (aplicada/pendiente/baseline) |
| `./gradlew validarMigraciones` | Valida checksums de los archivos contra lo aplicado en dev |

Además, **cualquier arranque de la app (`bootRun`) o ejecución de tests aplica automáticamente** lo pendiente — los comandos anteriores sirven para hacerlo/consultarlo sin levantar nada.

---

## 1. Flujo estándar: entidad o campo nuevo

Ejemplo real hipotético: agregar `estadio` a los equipos.

```sql
-- src/main/resources/db/migration/V2__agregar_estadio_a_equipos.sql
ALTER TABLE equipos ADD COLUMN estadio VARCHAR(120);
```

```java
// domain/model/Equipo.java — el campo en el dominio
public Equipo(UUID id, String nombre, String logoUrl, String estadio) { ... }
```

Pasos, en orden:

| # | Acción | Verificación |
|---|---|---|
| 1 | Escribe la Entity Java con el campo nuevo | Compila |
| 2 | Crea `V(n+1)__descripcion.sql` con el `ALTER TABLE` | El número `n+1` = último existente + 1 |
| 3 | Actualiza el adapter/entity JPA que mapea el campo | Compila |
| 4 | `./gradlew test` | Testcontainers aplica tu migración REAL + validate confirma Entity↔BD |
| 5 | `./gradlew bootRun` contra dev | Flyway aplica la migración en tu BD dev |

**Convención de nombres**: `V{n}__{verbo_objeto}.sql` en snake_case español — ej: `V3__agregar_estadio_a_equipos.sql`. Doble guion bajo (`__`) entre versión y descripción: obligatorio.

---

## 2. Cuando agregas COLUMNAS con datos existentes (patrón expandir-contrato)

⚠️ **Nunca agregues una columna `NOT NULL` sin valor por defecto sobre una tabla con filas**: la migración revienta si hay registros. El patrón seguro es en fases:

### Fase 1 — Expandir (migración segura)

```sql
-- V4__agregar_capacidad_estadio.sql
ALTER TABLE equipos ADD COLUMN capacidad INTEGER;          -- nullable primero
```

### Fase 2 — Backfill (rellenar los registros viejos)

```sql
-- V5__backfill_capacidad_estadio.sql
UPDATE equipos SET capacidad = 0 WHERE capacidad IS NULL;
```

*(Si el backfill depende de lógica de negocio compleja, valora hacerlo vía código/endpoint en vez de SQL.)*

### Fase 3 — Contrato (endurecer cuando ya todo está relleno)

```sql
-- V6__capacidad_obligatoria.sql
ALTER TABLE equipos ALTER COLUMN capacidad SET NOT NULL;
```

Cada fase puede ir en su propio deploy/release — así nunca tienes código Java nuevo escribiendo `NULL` en una columna que aún es nullable, ni una migración `NOT NULL` corriendo antes de que el backfill exista.

---

## 3. Registros actuales en dev — qué pasa con ellos

**Nada los toca.** Reglas que gobiernan tu BD dev desde H-05:

| Situación | Comportamiento |
|---|---|
| Arrancas la app en dev | Flyway marca/aplica solo migraciones pendientes; tus filas quedan intactas |
| Agregas una columna nullable | Los registros viejos quedan con `NULL` en ella (por eso la fase de backfill) |
| Agregas una TABLA nueva | Se crea vacía; se llena por uso (o poblamiento CU-10) |
| Re-ejecutas el poblamiento geográfico | Idempotente: no duplica países/ligas/equipos |

Los registros que hoy tienes en dev (países de interés, ligas, plantillas con escudos, tareas programadas, usuarios) **siguen siendo válidos y operativos** después de cada migración bien escrita.

---

## 4. Llevar datos de dev a producción

Concepto clave: **las migraciones transportan ESQUEMA, no datos de negocio.** El catálogo deportivo (países/ligas/equipos) se auto-pobla en producción con CU-10 desde las fuentes — no necesitas copiar esas filas. Lo que SÍ querrás preservar de dev son tus **configuraciones curadas**:

| Tabla | ¿Llevarla a prod? | Por qué |
|---|---|---|
| `usuarios` | ✅ Sí (al menos el SUPERADMIN) | Sin él no entras al panel |
| `paises_interes` | ✅ Sí — tus preferencias curadas | Evita reconfigurar "Mis preferidos" |
| `fuentes_extraccion` | ✅ Sí — catálogo de fuentes registrado | CU-04/CU-11 lo necesitan |
| `ligas` ACTIVAS + sus `detalle_fuentes_extraccion` (URLs pegadas a mano) | ✅ Sí — **horas de trabajo manual** | Cada URL la pegaste tú |
| `equipos`/`posiciones`/`partidos`/`cuotas` | ❌ No — se regeneran sincronizando | Dato operativo, no configuración |
| `tarea_log` | ❌ No | Histórico efímero |

### Procedimiento recomendado para producción

```bash
# 1. Producción arranca con BD VACÍA → bootRun aplica TODAS las migraciones desde V1
#    (esquema completo garantizado por Flyway, sin pasos manuales)

# 2. Exportar SOLO los datos de configuración desde dev:
pg_dump -h localhost -p 5434 -U postgres -d tipsterbytefxv2_dev \
  --data-only \
  -t usuarios -t paises_interes -t fuentes_extraccion \
  > datos_configuracion.sql

# 3. Importarlos en producción (después del primer arranque):
psql -h <prod-host> -U <usuario> -d <prod-db> < datos_configuracion.sql
```

Notas importantes:
- **IDs son UUID** generados aleatoriamente → sin problemas de secuencias/colisiones al importar
- Si importas `ligas` activas, importa también sus `temporadas`, `equipos` y `detalle_fuentes_extraccion` referenciados (respetando FKs) — o deja que el poblamiento las recree y solo migra `paises_interes` + `fuentes_extraccion` + `usuarios`
- Alternativa simple al dump: iniciar sesión en prod y recrear a mano preferencias + activar ligas (el sistema es idempotente y te guía)

---

## 5. Errores comunes y cómo salir de ellos

| Síntoma | Causa | Solución |
|---|---|---|
| Arranque falla: `Migration checksum mismatch for migration version X` | Editaste un `.sql` YA aplicado | Restaura el archivo a su contenido original y crea un `V(n+1)` con la corrección. (En dev desechable: `DELETE FROM flyway_schema_history WHERE version='X';` + `DROP` los objetos de esa migración y reaplica — solo si sabes lo que haces) |
| Arranque falla: `Schema-validation: missing column [x]` | Tu Entity tiene un campo que ninguna migración crea | Crea la migración pendiente con el `ALTER TABLE ... ADD COLUMN` |
| `Found non-empty schema without schema history table` | BD existente sin baseline | Ya cubierto con `baseline-on-migrate=true`; si creas una BD heredada nueva, aplica los mismos flags |
| Mi test unitario de adapter duerme 250ms por llamada | Cortesía H-06 activa | En tests está deshabilitada (`app.fuentes.cortesia.enabled=false`); si construyes un adapter a mano en un test, usa `ServicioCortesia.passthrough()` |

---

## 6. Checklist antes de commitear una migración

- [ ] El número `V(n+1)` es correlativo (revisa la carpeta `db/migration/`)
- [ ] No editaste ningún `.sql` ya commiteado/aplicado
- [ ] Probaste `./gradlew test` COMPLETO — Testcontainers ejecutó tu script desde cero
- [ ] Si la tabla tiene registros en dev: ¿nullable/backfill pensado (patrón expandir-contrato)?
- [ ] Header `[QUÉ]/[POR QUÉ]` en el `.sql` explicando el cambio (estándar del proyecto)
- [ ] Documentación afectada actualizada (modelo-dominio, comunicados si cambia contrato)

---

*Fuente de verdad backend: `src/main/resources/db/migration/` · Configuración: `spring.flyway.*` en `application.properties` · Diagnóstico original: H-05 en `docs/architecture/hallazgos-arquitectura.md`.*
