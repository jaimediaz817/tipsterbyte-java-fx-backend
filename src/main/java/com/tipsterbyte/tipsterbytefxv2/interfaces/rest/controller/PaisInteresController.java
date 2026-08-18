// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de países de interés (CU-14): registrar (POST), listar (GET),
//        eliminar (DELETE) y reemplazar la lista completa en orden (PUT).
// [POR QUÉ]: Es la puerta de entrada HTTP de la configuración de preferencia de
//            poblamiento: el frontend marca países disponibles y su orden antes de
//            sincronizar el catálogo (CU-10 les da prioridad sin omitir el resto).
//            El PUT recibe la lista completa ordenada = "guardar preferencias" en
//            bloque (la prioridad es la posición 1..n).
// [ALTERNATIVAS]: Un solo POST que reemplace; se descarta porque conviven el alta
//                 individual (agregar al final) y el guardado en bloque reordenado.
// [RELACIONES]: CU-14 → GestionarPaisesInteresUseCase; → PaisInteresRequest/Response.
//               Seguridad: SecurityConfig exige SUPERADMIN/TIPSTER para /api/v1/paises-interes/**.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarPaisInteresComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarPaisesInteresUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.PaisInteresRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PaisInteresResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paises-interes")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class PaisInteresController {

    private final GestionarPaisesInteresUseCase gestionarPaisesInteresUseCase;

    // [QUÉ]: Construye el controller con su caso de uso (inyección por constructor).
    public PaisInteresController(GestionarPaisesInteresUseCase gestionarPaisesInteresUseCase) {
        this.gestionarPaisesInteresUseCase = gestionarPaisesInteresUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/paises-interes — registra un país de interés al
    //        final de la lista (CU-14). Valida que exista en la fuente #1 (422 si no).
    //        Devuelve el país creado con su prioridad asignada (PaisInteresResponse)
    //        para que el frontend actualice el UI sin recalcular orden.
    @PostMapping
    public ResponseEntity<PaisInteresResponse> registrar(@Valid @RequestBody PaisInteresRequest request) {
        PaisInteres creado = gestionarPaisesInteresUseCase.registrar(new RegistrarPaisInteresComando(
                request.isoAlpha2(), request.nombre()));
        return ResponseEntity.created(URI.create("/api/v1/paises-interes"))
                .body(new PaisInteresResponse(creado.isoAlpha2(), creado.nombre(), creado.prioridad()));
    }

    // [QUÉ]: Endpoint GET /api/v1/paises-interes — lista los países de interés en
    //        orden de prioridad ascendente (CU-14).
    @GetMapping
    public ResponseEntity<List<PaisInteresResponse>> listar() {
        List<PaisInteresResponse> respuestas = gestionarPaisesInteresUseCase.listar().stream()
                .map(p -> new PaisInteresResponse(p.isoAlpha2(), p.nombre(), p.prioridad()))
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    // [QUÉ]: Endpoint DELETE /api/v1/paises-interes/{isoAlpha2} — elimina un país de
    //        interés (CU-14).
    @DeleteMapping("/{isoAlpha2}")
    public ResponseEntity<Void> eliminar(@PathVariable String isoAlpha2) {
        gestionarPaisesInteresUseCase.eliminar(isoAlpha2);
        return ResponseEntity.noContent().build();
    }

    // [QUÉ]: Endpoint PUT /api/v1/paises-interes — reemplaza la lista completa de
    //        países de interés con la lista ordenada enviada (prioridad = posición 1..n),
    //        eliminando los que ya no están (CU-14). Operación "guardar preferencias".
    @PutMapping
    public ResponseEntity<Void> reemplazarPreferencias(@Valid @RequestBody List<PaisInteresRequest> preferencias) {
        gestionarPaisesInteresUseCase.reemplazarPreferencias(preferencias.stream()
                .map(p -> new RegistrarPaisInteresComando(p.isoAlpha2(), p.nombre()))
                .toList());
        return ResponseEntity.noContent().build();
    }
}