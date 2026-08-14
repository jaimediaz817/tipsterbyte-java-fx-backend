---
name: diagramas-interactivos
description: Use when creating or editing diagrams (HTML/SVG) that should include interactive detail: click a class/artifact to open a modal dialog with QUÉ/RELACIÓN/REGLAS/MÉTODOS/EVENTOS, and click a relationship to show the connecting attribute. Complements diagram-design with the interactivity layer.
---

# Diagramas Interactivos — tipsterbyte-fx-v2

## Cuándo usar — OBLIGATORIO en todo diagrama

**Todo diagrama** que se genere en el proyecto (modelo de dominio, arquitectura, flujos de casos de uso, ERD de FASE 8, etc.) debe usar esta skill. No es opcional: **los elementos principales del diagrama deben ser clicables** y su detalle explicarse en un dialog modal.

Complementa al skill `diagram-design` (estilo visual): se cargan ambos. `diagram-design` define el look (paleta, tipografía, tipos de diagrama); esta skill define la capa de interacción. La convención está fijada en `AGENTS.md` (§ Convención de diagramas interactivos) y en `.opencode/rules/documentation.md`.

## Principio rector

**Un diagrama no debe explicarlo todo en el lienzo.** El lienzo muestra la vista estructural (qué hay y cómo se conecta); el detalle vive detrás de un click. Esto mantiene la densidad baja (regla del skill diagram-design) y el diagrama usable.

## Contrato de interacción

Todo artefacto clicable debe ser un `<g>` con:

- Clase `clase` (cajas/artefactos) o `relacion` (conectores).
- Atributos `data-*` (siempre en español):

| Atributo | Contenido |
| --- | --- |
| `data-titulo` | Nombre del artefacto o de la relación (ej: `Liga`, `Partido — Pronostico`) |
| `data-tipo` | Tipo en `mono` (ej: `Aggregate root · domain.model`, `Relación 1:N · ref. por id`, `Composición · miembro del aggregate`) |
| `data-que` | Responsabilidad del artefacto en 1-2 líneas (estándar `[QUÉ]`) |
| `data-relacion` | **Para relaciones**: atributo de conexión (ej: `Pronostico.partidoId → Partido.id`). **Para clases**: con qué se relaciona y por qué atributo |
| `data-regla` | Reglas de negocio asociadas (BR-xx) o `—` si no aplica |
| `data-metodos` | Métodos públicos de negocio o `—` |
| `data-eventos` | Eventos de dominio que emite o `—` |

## Reglas de visibilidad (no sacrificar el diagrama)

- El hover **solo indica clicabilidad**: `cursor: pointer`, borde más grueso y subrayado del título. **Nunca** atenuar/ocultar el contenido de otros elementos.
- El detalle se muestra en un **dialog modal** al hacer click, nunca en un tooltip flotante al hover (activa por accidente y cubre el diagrama).
- El dialog se cierra con: botón ✕, click fuera del panel o tecla `Escape`.

## Sin salto de scroll al abrir/cerrar el modal (OBLIGATORIO)

Al abrir el modal se bloquea el scroll de fondo (`overflow: hidden`). Si se hace de forma directa, la barra de scroll desaparece y **todo el contenido salta hacia la derecha**; al cerrar, vuelve a saltar. Prohibido ese comportamiento.

**Solución (doble capa):**

1. **CSS nativo** — reservar siempre el espacio de la barra, aunque esté oculta o no haya scroll:
```css
html { scrollbar-gutter: stable; }
```
2. **Fallback JS** — solo si `scrollbar-gutter` no está soportado: medir el ancho de la barra **antes** de ocultarla y compensar con `padding-right`:
```js
var soportaGutter = window.CSS && CSS.supports && CSS.supports('scrollbar-gutter', 'stable');
function bloquearScroll() {
  if (!soportaGutter) {
    var anchoBarra = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.paddingRight = anchoBarra + 'px';
  }
  document.documentElement.style.overflow = 'hidden';
}
function liberarScroll() {
  document.body.style.paddingRight = '';
  document.documentElement.style.overflow = '';
}
```

Verificar siempre que abrir/cerrar el modal **no produzca desplazamiento horizontal** del lienzo. Si aun así se ve salto: hacer **hard refresh (Ctrl+F5)** para descartar caché del navegador.

## Lienzo con scroll horizontal (OBLIGATORIO)

Los diagramas usan SVG con `viewBox` de ancho fijo (ej: `0 0 1320 936`). **Prohibido** comprimir el SVG por debajo de su ancho natural con `min-width` bajo: los textos de las relaciones se solapan y se cortan.

**Regla**: envolver el SVG en un contenedor con `overflow-x: auto` y darle al SVG su ancho natural (el del `viewBox`):

```html
<div class="lienzo">
  <svg viewBox="0 0 1320 936" ...>
    <!-- ... -->
  </svg>
</div>
```

```css
.lienzo { overflow-x: auto; margin-top: 1.5rem; }
svg { width: 100%; min-width: 1320px; display: block; }
```

El `min-width` del SVG debe ser **el ancho del `viewBox`** (no 1100px ni otro menor). En pantallas anchas el SVG se estira hasta el ancho máximo del `.frame`; en angostas aparece la barra de scroll horizontal y el diagrama nunca pierde legibilidad. El `body` no debe centrar con `flex` el contenido que desborda (corta ambos lados); usar `.frame { margin: 0 auto; }`.

## Estructura del dialog

Filas etiquetadas con el estándar del proyecto:

```
QUÉ · RELACIÓN · REGLAS · MÉTODOS · EVENTOS
```

- `REGLAS` pinta cada `BR-0xx` como chip (borde accent + fondo tint).
- Código y nombres de atributos van en `<code>` mono.
- Fondo blanco, borde superior accent, sombra — coherente con la paleta de `diagram-design`.

## Plantilla mínima de JS

```html
<script>
  (function () {
    var overlay = document.getElementById('overlay');
    var campos = { /* ids de las filas del dialog */ };
    function abrir(el) {
      // rellenar campos desde los data-* y mostrar overlay
    }
    document.querySelectorAll('.clase, .relacion').forEach(function (el) {
      el.addEventListener('click', function () { abrir(el); });
    });
    // cerrar: botón, click fuera, Escape
  })();
</script>
```

## Referencias

- Ejemplo completo funcional: `diagrams/modelo-dominio.html` (aggregates Liga/Partido/Pronostico/Suscripcion con click en clases y relaciones).
- Paleta y tipografía: `.opencode/skills/diagram-design/references/style-guide.md`.
- Regla de documentación `[QUÉ]/[POR QUÉ]/[ALTERNATIVAS]/[RELACIONES]`: `.opencode/rules/documentation.md`.
- Contenido semántico del dominio: `docs/domain/modelo-dominio.md`.

## Qué evitar

- Tooltips flotantes como mecanismo principal de detalle.
- Datos `data-*` en inglés o sin los 5 campos (dejar `—` explícito cuando no aplique).
- Atributos inventados que no existan en el código (el diagrama debe reflejar el código real).
