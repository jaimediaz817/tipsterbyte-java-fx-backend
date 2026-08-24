---
name: migrations-ddl-flyway-auto
description: Use PROACTIVELY whenever a domain entity, JPA entity, field, relation, enum value or table changes in this repository — automatically proposes and creates the corresponding Flyway migration (V(n+1)__*.sql) following project DDL conventions, before running tests. Also use when asked about schema changes, columns, constraints or data backfills.
---

# Migrations DDL Flyway Auto — tipsterbyte-fx-v2

## Cuándo usar (TRIGGER automático)

ACTIVA este skill SIEMPRE que el usuario pida o tú detectes CUALQUIERA de estos cambios:

| Cambio en el código | Implicación de esquema |
|---|---|
| Campo nuevo/modificado/eliminado en un `domain.model.*` que persiste | `ADD COLUMN` / `ALTER COLUMN` / `DROP COLUMN` |
| Entidad nueva (`@Entity`) | `CREATE TABLE` + FKs |
| Relación nueva o modificada (@ManyToOne/@OneToMany) | FK nueva / cambio de constraint |
| Valor nuevo en un **enum persistido** (@Enumerated) | ⚠️ Actualizar CHECK de la columna (ver Gotcha 2) |
| Tabla nueva | `CREATE TABLE` |

Al terminar de implementar el cambio de código, la migración DEBE existir antes de correr tests. NO esperes a que el usuario la pida: **propón y crea el `.sql` proactivamente** como parte del mismo trabajo.

---

## Flujo obligatorio (5 pasos)

### Paso 1 — Crear el archivo con numeración automática

```bash
./gradlew nuevaMigracion -Pdescripcion=verbo_objeto_en_snake_case
```

Ejemplos: `-Pdescripcion=agregar_estadio_a_equipos`, `-Pdescripcion=eliminar_campo_prueba`.
NUNCA numeres a mano. NUNCA edites un `.sql` ya aplicado.

### Paso 2 — Escribir el DDL según los patrones de abajo

Header obligatorio ([QUÉ]/[POR QUÉ]/[RELACIONES]) + SQL idempotente cuando aplique.

### Paso 3 — Actualizar código

Entity JPA (`infrastructure/persistence/entity/*`) + mappers de adapter + dominio si aplica.

### Paso 4 — Validar

```bash
./gradlew test        # Testcontainers aplica tu migración desde cero; validate confirma Entity↔BD
```

Si falla con `Schema-validation: missing column [x]` → tu migración está incompleta.

### Paso 5 — Aplicar en dev (opcional inmediato)

```bash
./gradlew migrar      # o deja que bootRun lo haga
./gradlew infoMigraciones   # confirmar estado
```

---

## Patrones DDL obligatorios (convenciones del proyecto)

### Nombres de constraints

| Tipo | Patrón | Ejemplo |
|---|---|---|
| Foreign key | `fk_<tabla>_<referencia>` | `fk_equipos_temporada` |
| Unique | `uk_<tabla>_<columnas>` | `uk_detalle_fuente_temporada_tipo` |
| Check (enum) | inline en columna, sin nombre custom (estilo Hibernate) | ver Gotcha 2 |

### Columnas nuevas sobre tablas CON registros → expandir-contrato

```sql
-- FASE 1: nullable primero (seguro con filas existentes)
ALTER TABLE equipos ADD COLUMN capacidad INTEGER;

-- FASE 2: backfill si hay valor razonable (migración separada)
UPDATE equipos SET capacidad = 0 WHERE capacidad IS NULL;

-- FASE 3: endurecer (solo cuando el código Java ya no escribe NULL)
ALTER TABLE equipos ALTER COLUMN capacidad SET NOT NULL;
```

### Relaciones (FK)

```sql
ALTER TABLE <tabla> ADD COLUMN <referencia>_id UUID;             -- o el tipo de id
ALTER TABLE <tabla>
  ADD CONSTRAINT fk_<tabla>_<referencia>
  FOREIGN KEY (<referencia>_id) REFERENCES <tabla_ref>(id);
```

### ⚠️ GOTCHA 1 — Enum persistido con valor NUEVO o MODIFICADO

Los enums generan CHECK inline (`check ((tipo in ('STANDINGS',...)))`). Al añadir/quitar un valor:

```sql
-- 1. Quitar el CHECK viejo (nombre autogenerado: consultarlo en dev con DBeaver o:
--    SELECT conname FROM pg_constraint WHERE conrelid = 'detalle_fuentes_extraccion'::regclass;
ALTER TABLE detalle_fuentes_extraccion DROP CONSTRAINT detalle_fuentes_extraccion_tipo_check;

-- 2. Recrearlo con la lista COMPLETA actualizada
ALTER TABLE detalle_fuentes_extraccion ADD CONSTRAINT detalle_fuentes_extraccion_tipo_check
  CHECK ((tipo in ('STANDINGS','ODDS_WPLAY','CALENDAR','EQUIPOS','<NUEVO>')));
```

Referencia completa: ADR-008 en `docs/architecture/arquitectura-objetivo.md`.

### ⚠️ GOTCHA 2 — Eliminar columnas/campos

En la MISMA migración: `DROP COLUMN x;` + eliminar cualquier índice/constraint dependiente. Y revertir el campo en Entity JPA + dominio + mappers + tests en el mismo commit.

### ❌ Prohibido

- Editar un `.sql` YA aplicado (checksum mismatch → arranque roto). Corregir = nuevo V(n+1).
- `NOT NULL` sin default sobre tabla con filas (usa expandir-contrato).
- Borrar/renombrar columnas sin plan de datos (¿backfill? ¿se pierde info valiosa?).

---

## Checklist final antes de commitear

- [ ] Migración creada vía `nuevaMigracion` (numeración correlativa)
- [ ] Header [QUÉ]/[POR QUÉ]/[RELACIONES] completo en el `.sql`
- [ ] Entity JPA + mappers + dominio actualizados en el mismo commit
- [ ] Si el enum cambió: CHECK recreado (Gotcha 1)
- [ ] `./gradlew test` completo en verde
- [ ] Documentación afectada tocada (modelo-dominio, comunicados si cambia contrato REST)

---

*Ver también: manual operativo completo en `docs/migration/README.md` · Diagnóstico origen: H-05 en `docs/architecture/hallazgos-arquitectura.md`.*
