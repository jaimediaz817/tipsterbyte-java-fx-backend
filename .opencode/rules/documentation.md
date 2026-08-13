# Regla de Documentación — tipsterbyte-fx-v2

> Regla SIEMPRE obligatoria. Cada artefacto codificado documenta QUÉ es, POR QUÉ se usó, qué ALTERNATIVAS hubo y con qué se RELACIONA.

## Estándar obligatorio (cabecera en cada clase/interface/artefacto)

```java
// ─────────────────────────────────────────────
// [QUÉ]: Responsabilidad del artefacto en 1-2 líneas.
// [POR QUÉ]: Decisión de diseño que lo justifica.
// [ALTERNATIVAS]: (opcional) opciones descartadas y motivo.
// [RELACIONES]: Con qué casos de uso (CU-xx), puertos, adapters
//               o agregados de dominio se conecta.
// ─────────────────────────────────────────────
```

- Aplicar a: clases, interfaces, métodos públicos no triviales, configuraciones y archivos de infraestructura.
- Comentarios y docs en **español**.
- Un commit = una unidad lógica de cambio, con mensaje descriptivo en español.

## Trazabilidad

- Todo caso de uso implementado debe referenciar su **HU** (HU-xx) y **CU** (CU-xx).
- Toda decisión técnica significativa debe registrar su **ADR** en `docs/architecture/arquitectura-objetivo.md`.

## Referencias

- `docs/use-cases/historias-de-usuario.md` — matriz HU → CU → puertos.
- `docs/use-cases/casos-de-uso.md` — catálogo CU-01..09.
- `AGENTS.md` — contexto de proyecto.