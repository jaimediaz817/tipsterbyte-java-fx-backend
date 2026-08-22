# Diagnóstico — Excusa hexagonal vía Notificaciones (sin dañar lo funcional)

> Estado: 📝 Diagnóstico para fase de aprendizaje — no implementado, solo diseñado como extensión hexagonal portátil.

---

## 1. Contexto actual — qué hexagonal ya existe

```
interfaces (REST/DTOs) → application (use cases + ports) → domain (núcleo) ← infrastructure (adapters)
```

| Capa | Artefacto clave hoy | Patrón POO ya en uso |
|---|---|---|
| `domain` | Aggregates `Liga`, `Partido` (con `Temporada` como Entity), `Pronostico`, `Suscripcion`, VOs, `DomainEvent` | **Aggregate**, **Factory Method** (`reconstruir`), **State** (`EstadoLiga`, `EstadoTemporada.activar/finalizar`) |
| `application` | CU-01..16 + ports `Proveedor*`/`*Repository`/`CacheLecturas` | **Port/Adapter (Hexagonal)**, **Strategy** (cambiar proveedor = nuevo adapter), **Use Case as Transaction Script** |
| `infrastructure` | `*Cacheable` decoradores, `*JpaAdapter`, `*Adapter` HTTP | **Decorator** (cache-aside), **Adapter**, **Repository** |
| Transversal | `DomainEvent` + `pullEventos()` | **Observer** embrionario (eventos emitidos, aún sin suscriptores) |

El proyecto **ya es hexagonal**. La excusa no es "meter hexagonal", sino **profundizarlo** con un caso de uso transversal que lo evidencie sin tocar el flujo crítico (activar liga, poblar, sincronizar).

---

## 2. [QUÉ] — Caso de uso excusa propuesto: Notificaciones de dominio

**CU-17 — Notificar a interesados cuando ocurre un evento de negocio**

Dos disparadores concretos y de valor real (no inventados):

| Disparador | Evento existente | Interesado | Canal |
|---|---|---|---|
| A. `PronosticoPublicado` (CU-07) | `Pronostico` pasa `BORRADOR → PUBLICADO` | Clientes con `Suscripcion ACTIVA` al tipster | In-App + Email (opt-in) |
| B. Fallo de sincronización (CU-01/02/03/10) | `InfraestructureException` capturada con `log.warn` | SUPERADMIN/TIPSTER (operador) | In-App (bandeja) + Email opcional |

**Criterios de aceptación (excusa mínima viable):**
- AC1: Publicar pronóstico genera 1 notificación por suscriptor activo (sin duplicar si ya fue notificado).
- AC2: Fallo de fuente genera 1 notificación de error para operadores (con `ligaId`, `tipoFuente`, `mensaje`).
- AC3: Notificar **no bloquea** el caso de uso principal (si el envío falla, el pronóstico sigue publicado).
- AC4: Preferencias por usuario (qué canal quiere) sin acoplar el dominio.

---

## 3. [POR QUÉ] — Por qué notificaciones es la excusa perfecta para POO + hexagonal

1.  **Es transversal y desacoplable**: no modifica `Liga` ni `Pronostico`; se suscribe a sus `DomainEvent`s. Si lo quitas, el negocio sigue funcionando. Riesgo cero para lo funcional.
2.  **Obliga a usar el `domain` sin contaminarlo**: el dominio solo emite eventos; no conoce `JavaMailSender`, `WebSocket` ni `Firebase`.
3.  **Fuerza Strategy + Factory en estado puro**: cada canal es una estrategia intercambiable; la factoría elige canales según preferencias.
4.  **Reutiliza lo que ya existe**: `DomainEvent`, `pullEventos()`, `TareaLog` (observabilidad) y el scheduler (para reintentos) ya están.

---

## 4. Diseño hexagonal propuesto (sin dañar lo funcional)

```
domain/model: Notificacion (Entity, id, destinatarioId, tipo, payload, estado, intentos)
domain/event: PronosticoPublicado, SincronizacionFallida (ya existen + 1 nuevo)
domain/service: PoliticaNotificacion (decide a quién notificar)

application/port: Notificador (port) — void notificar(Notificacion n)
                + NotificacionRepository (port) — guardar/buscarPorDestinatario
application/usecase: NotificarPronosticoUseCase (CU-17A, listener de evento)
                     NotificarFalloSincronizacionUseCase (CU-17B)

infrastructure/adapter: EmailNotificadorAdapter implements Notificador
                        InAppNotificadorAdapter implements Notificador
                        CompositeNotificador (elige canales según preferencia)
                        NotificacionRepositoryJpaAdapter
```

**Flujo (Observer asíncrono, no bloqueante):**

```
CU-07.publicar() → pronostico.publicar() → emite PronosticoPublicado
       → guarda pronostico
       → ApplicationEventPublisher publica evento (Spring)
         → Listener @TransactionalEventListener(AFTER_COMMIT) invoca CU-17A
           → por cada suscriptor activo: crea Notificacion → Notificador.notificar()
             (si un canal falla, marca reintento; nunca revierte CU-07)
```

**Puntos finos de POO eficaz:**

| Patrón | Dónde luce |
|---|---|
| **Strategy** | `Notificador` con `EmailNotificador` vs `InAppNotificador` vs `Composite` |
| **Factory Method** | `NotificacionFactory.crearParaPronostico(pronostico, suscriptor)` encapsula reglas |
| **Observer** | `DomainEvent` → listener desacoplado (ya existe `pullEventos()`) |
| **Decorator** | Reintento con backoff envolviendo `Notificador` (igual que `Proveedor*Cacheable`) |
| **Adapter** | Cada canal adapta librería externa (JavaMail, WebSocket) al port |

---

## 5. [ALTERNATIVAS] descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Llamar a `emailService.send()` directo desde `Pronostico.publicar()` | Contamina el dominio con infraestructura; viola Dependency Rule. |
| Usar `@Async` dentro del mismo caso de uso | Acopla la orquestación; el listener mantiene CU-07 limpio y testeable sin Spring. |
| Tabla `notificaciones` sin aggregate | Se pierde trazabilidad de intentos/estado; Notificacion como Entity con ciclo de vida es más expresiva. |
| WebSocket directo sin port | Acopla a una tecnología; Strategy permite añadir Push/FCM mañana sin tocar dominio. |

---

## 6. [RELACIONES] — qué toca y qué NO toca

- **Toca (nuevo, sin modificar lo existente):** `domain/model/Notificacion`, `domain/service/PoliticaNotificacion`, `application/port/Notificador`, 2 use cases nuevos, 3 adapters nuevos, 1 tabla `notificaciones`.
- **NO toca:** `Liga`, `Partido`, `Temporada`, `ActivarLigaUseCase`, `Sincronizar*`, BR-001..008, `TipoFuenteExtraccion`, `fuentes-externas.md`. Solo **lee** sus eventos.
- **Reutiliza:** `DomainEvent`, `PronosticoRepository`, `SuscripcionRepository`, `TareaLog` (para correlacionar), `SecurityConfig` (preferencias por `Usuario`).

---

## 7. Qué practicarías con esto (valor de aprendizaje)

1.  **Hexagonal puro**: dominio sin imports de Spring/JPA; ports en `application`, adapters en `infrastructure`.
2.  **POO eficaz**: inmutabilidad donde toca (VOs), identidad donde toca (Entities), composición sobre herencia (`Liga` compone `Temporada` que compone `Notificacion` es anti-patrón — aquí cada aggregate es independiente).
3.  **Eventos de dominio como contrato**: `PronosticoPublicado` ya existe; añadir `SincronizacionFallida` es el mismo gesto que hicimos con `LigaActivada`.
4.  **Patrones combinados**: Strategy + Factory + Observer + Decorator en un mismo flujo transversal, sin over-engineering.

---

## 8. Siguiente paso propuesto (cuando apruebes)

1. Crear rama `feat/notificaciones-hexagonal`.
2. FASE N — Dominio: `Notificacion` + `PoliticaNotificacion` + evento `SincronizacionFallida` + tests.
3. FASE N+1 — Application/Infrastructure: ports + 2 adapters + CU-17A/B + wiring + tabla.
4. Comunicado frontend: `GET /api/v1/notificaciones` (bandeja) + badge + preferencia de canal.

> Este documento no modifica código. Es la base para decidir si abrimos esa FASE y en qué orden.
