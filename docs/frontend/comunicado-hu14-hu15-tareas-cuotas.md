# Comunicado HU-FRONT — Tareas Programadas v2 (HU-14) + Cuotas Próximas con Volatilidad (HU-15)

> [QUÉ]: Guía de presentación y acciones para las dos nuevas historias, con shapes de
>         API nuevos/extendidos verificados contra el diseño backend acordado.
> [POR QUÉ]: El panel actual de tareas programadas crece: pasa de lista plana a una
>             vista LIGA-CÉNTRICA (liga = nodo raíz con sus tareas dentro) y nace un
>             panel de cuotas próximas con indicadores de volatilidad. Este doc propone
>             estructura y acciones desde backend; el detalle visual es criterio del
>             equipo frontend.
> [RELACIONES]: docs/use-cases/historias-de-usuario.md HU-13/HU-14/HU-15 · comunicado-
>               tareas-programadas.md (v1, vigente como base) · comunicado-fuentes-
>               extraccion.md. ⚠️ Requiere backend con HU-14/HU-15 implementadas.

---

## 1. Vista "Tareas Programadas" — propuesta de reorganización (HU-14)

### Estructura sugerida: tabla agrupada por liga activa

```
┌─ LIGA PROFESIONAL (Argentina) ─────────────── [⏸ Pausar todas] [▶ Reanudar todas]
│   Estado global: ● ACTIVA · próxima ejecución ~18:00
│   ┌──────────────┬───────────┬──────────────┬─────────────┬──────────────┬─────────┐
│   │ Fuente       │ Frecuencia│ Primer disp. │ Último res. │ Último log   │ Acciones│
│   ├──────────────┼───────────┼──────────────┼─────────────┼──────────────┼─────────┤
│   │ ODDS_WPLAY   │ cada 1 h  │ hoy 18:00    │ ✅ SUCCESS 0 │ hace 32 min  │ ⏸ 📜 ✏️ │
│   │ STANDINGS    │ cada 3 d  │ mañ. 06:00   │ ✅ SUCCESS 20│ ayer         │ ▶ 📜 ✏️ │
│   │ CALENDAR     │ cada 12 h │ —            │ ❌ ERROR    │ hace 5 h     │ ▶ 📜 ✏️ │
│   └──────────────┴───────────┴──────────────┴─────────────┴──────────────┴─────────┘
└─ (siguiente liga activa...)
```

- **Una fila por tarea** (liga × tipoFuente); **cabecera por liga** con acciones masivas.
- Acciones por fila: pausar/reanudar individual (`PUT /{id}`), ver logs (`GET /{id}/logs`),
  editar (`PUT /{id}`: frecuencia amigable, primer disparo).
- Acciones por cabecera de liga: pausa/reanudación MASIVA (`PUT /liga/{ligaId}/estado`).

### Datos nuevos disponibles (backend)

| Dato | Endpoint | Nota |
| --- | --- | --- |
| Tareas filtradas por liga | `GET /api/v1/tareas-programadas?ligaId={id}` | NUEVO query param |
| Pausa/reanudación masiva | `PUT /api/v1/tareas-programadas/liga/{ligaId}/estado` body `{"activa": false}` | NUEVO · 200 con `{ligaId, activa, tareas:[{tipoFuente, activa}...]}` · 404 sin tareas |
| Primer disparo | campo `primerDisparo` (ISO-8601, opcional) en POST/PUT | NUEVO · mientras `now < primerDisparo` la tarea NO corre |
| Frecuencia amigable | `{"frecuencia": {"valor": 1, "unidad": "HORAS"}}` | Igual que v1 (VO Frecuencia → cron) |

### Modal de creación liga-céntrico (reemplaza al select plano)

```
1. Seleccionar LIGA (solo activas)            ← GET /ligas?estado=ACTIVA
2. El sistema muestra sus fuentes disponibles ← GET /tareas-programadas/disponibles
   (ya filtra detalles inactivos y marca duplicadas)
3. Por fuente elegida: frecuencia (valor+unidad) + primer disparo (datetime-local)
4. Guardar → POST /tareas-programadas
```

Regla UX clave: **guardar nunca dispara de inmediato** — la primera corrida ocurre en
`max(primerDisparo, próximo match del cron)`. Sugerimos mostrar ese cálculo como texto
de ayuda ("iniciará el mar 18:00").

---

## 2. Panel "Cuotas Próximas" (HU-15)

### Propuesta: tabla compacta con badge de volatilidad + drill-down

```
┌ CUOTAS PRÓXIMAS — Liga Profesional · ventana 24h ──────────── [24h ▾] ┐
│ Partido                    │ Fecha        │ 1X2 (L/E/V)      │ Señal │
│ Fluminense vs Palmeiras    │ sáb 19:30    │ 2.72/3.15/2.65   │ 🔴 VOLÁTIL │
│ Boca vs River              │ dom 17:00    │ 2.10/3.00/3.40   │ 🟢 ESTABLE │
│ Nacional vs Peñarol        │ dom 20:00    │ 2.85/3.10/2.60   │ 🟠 MODERADA│
│ Medellín vs Caldas         │ lun 20:00    │ —                │ ⚪ SIN DATOS│
└─ (click en fila → expande serie horaria por mercado) ─────────────────┘
```

- **Solo snapshot reciente por partido** (no listado crudo de capturas — acordado).
- Badge derivado del campo server-side `volatilidad`: `ESTABLE`→verde, `MODERADA`→ámbar,
  `VOLATIL`→rojo, `SIN_BASELINE`→gris. **No calcular nada en cliente**: umbrales viven
  en properties del backend; junto a `volatilidad` viene `variacionPorcentual` (null si
  SIN_BASELINE) por si quieren mostrarlo en tooltip.

### Endpoints (NUEVOS, llegan con HU-15)

```
GET /api/v1/ligas/{ligaId}/cuotas-proximas?ventanaHoras=24
Roles: SUPERADMIN | TIPSTER
→ [{ partidoId, equipoLocal, equipoVisitante, fechaUtc, jornada,
     cuotas: [{mercado, seleccion?, valor}], volatilidad, variacionPorcentual }]

GET /api/v1/partidos/{partidoId}/cuotas/historial?horas=24&mercado=UNO_X_DOS
Roles: SUPERADMIN | TIPSTER
→ serie cronológica [{capturadaEn, mercado, seleccion?, valor}] para gráfico/detalle
```

Sugerencia de interacción: click en fila expande inline el historial (drill-down),
con selector de mercado y ventana (6h/24h/72h).

---

## 3. Estados y errores a contemplar

| Situación | HTTP | Sugerencia UI |
| --- | --- | --- |
| Doble click en "cargar equipos"/sync simultáneo | 409 | Toast informativo, no error duro |
| Liga sin tareas al pausar masivamente | 404 | Ocultar acciones masivas si grupo vacío |
| Tarea creada pero aún no llega su primer disparo | — | Badge "programada, inicia {fecha}" |
| Corrida SUCCESS con 0 elementos (Wplay sin jornada montada) | — | Mostrar "sin datos aún" (no es fallo) |
| Detalle fuente desactivado (la tarea se omite sola) | — | Badge "fuente inactiva" en la fila |

---

## 4. Preguntas abiertas — esperamos su respuesta 👇

1. ¿Les sirve la agrupación liga → filas por fuente, o prefieren otra jerarquía?
2. Para el "próximo disparo estimado" de cada tarea: ¿lo necesitan calculado por el
   backend (podemos exponer `proximaEjecucion` derivada de cron+primerDisparo) o lo
   muestran solo tras la primera corrida usando logs?
3. Ventana por defecto del panel de cuotas: ¿24h les parece o prefieren otro default?
4. ¿Polling cada X segundos para el panel de cuotas (estilo catálogo geográfico) o
   refresco manual/botón?
5. ¿Necesitan endpoint adicional para listar SOLO las fuentes activas de una liga
   (sin tareas) en el modal de creación, o `/disponibles` les alcanza?

Mientras responden, el backend procede con la implementación de HU-14 (los shapes de
HU-15 se confirman al cerrar estas preguntas).
