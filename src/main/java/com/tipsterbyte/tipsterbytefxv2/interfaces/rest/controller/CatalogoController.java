// ─────────────────────────────────────────────
// [QUÉ]: Controller REST del catálogo geográfico (CU-10): dispara la sincronización
//        de países y ligas desde las fuentes #1/#5 y expone el estado del catálogo.
// [POR QUÉ]: Es el "momento 0" del panel del SUPERADMIN: un botón dispara POST
//            /activar y consulta GET /estado para saber si el catálogo está poblado.
//            La sincronización es síncrona e idempotente (CU-10), por lo que el
//            estado tras la activación refleja los conteos reales persistidos.
// [ALTERNATIVAS]: Activación asíncrona con tracking de progreso; se descarta porque
//                 CU-10 es síncrono y el volumen (176 países) no lo justifica aún.
// [RELACIONES]: CU-10 → SincronizarCatalogoUseCase + ConsultarEstadoCatalogoUseCase
//               → CatalogoEstadoResponse. Acceso solo SUPERADMIN.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CatalogoEstadoResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogo")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class CatalogoController {

    private final SincronizarCatalogoUseCase sincronizarCatalogoUseCase;
    private final ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase;

    // [QUÉ]: Construye el controller con sus casos de uso (inyección por constructor).
    public CatalogoController(SincronizarCatalogoUseCase sincronizarCatalogoUseCase,
                              ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase) {
        this.sincronizarCatalogoUseCase = sincronizarCatalogoUseCase;
        this.consultarEstadoCatalogoUseCase = consultarEstadoCatalogoUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/catalogo/activar — dispara la sincronización del
    //        catálogo (CU-10) y devuelve el estado resultante.
    // [POR QUÉ]: Es el "momento 0": el SUPERADMIN pulsa el botón del panel y el
    //            backend puebla países y ligas desde las fuentes #1/#5.
    @PostMapping("/activar")
    public ResponseEntity<CatalogoEstadoResponse> activar() {
        sincronizarCatalogoUseCase.ejecutar();
        return ResponseEntity.ok(toResponse(consultarEstadoCatalogoUseCase.ejecutar()));
    }

    // [QUÉ]: Endpoint GET /api/v1/catalogo/estado — devuelve el estado actual del
    //        catálogo (VACIO/POBLADO) con los conteos de países y ligas.
    // [POR QUÉ]: El frontend consulta este estado al cargar el panel para saber si
    //            el catálogo ya fue poblado y habilitar el flujo de activación.
    @GetMapping("/estado")
    public ResponseEntity<CatalogoEstadoResponse> estado() {
        return ResponseEntity.ok(toResponse(consultarEstadoCatalogoUseCase.ejecutar()));
    }

    private CatalogoEstadoResponse toResponse(com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto dto) {
        return new CatalogoEstadoResponse(dto.estado(), dto.totalPaises(), dto.totalLigas());
    }
}