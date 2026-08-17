// ─────────────────────────────────────────────
// [QUÉ]: Controller REST del catálogo geográfico de países: expone GET /api/v1/paises
//        (listado con filtros opcionales por continente y mapeado) para el panel de
//        administración del frontend (países → ligas por país → activación CU-04) y
//        GET /api/v1/paises/disponibles (catálogo completo de la fuente #1, sin persistir).
// [POR QUÉ]: El catálogo de países lo puebla CU-10 (SincronizarCatalogoUseCase, fuente
//            #1 ext-soccerway-countries) pero no estaba expuesto por API. Es una vista
//            de solo lectura sobre un puerto que ya existe (PaisRepository.buscarTodos()),
//            sin lógica de negocio nueva. El orden alfabético (case-insensitive) por
//            nombre se aplica en memoria: ~176 registros, no justifica paginación ni
//            ORDER BY en BD. El endpoint de disponibles da al frontend la lista completa
//            de la fuente para elegir países de interés (CU-14) antes de poblar.
// [ALTERNATIVAS]: Agregar el endpoint a LigaController; se descarta porque son recursos
//                 distintos (países ≠ ligas) y violaría responsabilidad única. Filtros
//                 en el repositorio (findByContinente...); se descartan por simplicidad:
//                 el volumen es pequeño y los filtros son opcionales.
// [RELACIONES]: PaisRepository (puerto CU-10) + ProveedorPaises (fuente #1) →
//               PaisResponse/PaisDisponibleResponse. Seguridad: SecurityConfig
//               exige SUPERADMIN/TIPSTER para /api/v1/paises/**.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PaisDisponibleResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PaisResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paises")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class PaisController {

    private final PaisRepository paisRepository;
    private final ProveedorPaises proveedorPaises;

    public PaisController(PaisRepository paisRepository, ProveedorPaises proveedorPaises) {
        this.paisRepository = paisRepository;
        this.proveedorPaises = proveedorPaises;
    }

    // [QUÉ]: Endpoint GET /api/v1/paises — lista el catálogo de países con filtros
    //        opcionales (continente, mapeado), ordenado alfabéticamente por nombre
    //        (case-insensitive). Devuelve [] con 200 si el catálogo está vacío.
    @GetMapping
    public ResponseEntity<List<PaisResponse>> listarPaises(
            @RequestParam(required = false) String continente,
            @RequestParam(required = false) Boolean mapeado) {
        List<PaisResponse> paises = paisRepository.buscarTodos().stream()
                .filter(pais -> continente == null || continente.isBlank()
                        || pais.continente().equalsIgnoreCase(continente))
                .filter(pais -> mapeado == null || pais.mapeado() == mapeado)
                .sorted(Comparator.comparing(Pais::nombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toPaisResponse)
                .toList();
        return ResponseEntity.ok(paises);
    }

    // [QUÉ]: Endpoint GET /api/v1/paises/disponibles — lista el catálogo completo de
    //        países de la fuente #1 (sin persistir) para que el frontend elija los
    //        países de interés (CU-14) antes de poblar el catálogo.
    // [POR QUÉ]: Con solo 1 país persistido (el catálogo se puebla con CU-10) la vista
    //            de países persistidos no basta para elegir preferencias; aquí se
    //            expone la disponibilidad real de la fuente sin escribir en BD.
    // [ALTERNATIVAS]: Un endpoint dedicado en PaisInteresController; se descarta porque
    //                 es un recurso de países (disponibles), no de la lista de interés.
    @GetMapping("/disponibles")
    public ResponseEntity<List<PaisDisponibleResponse>> listarDisponibles() {
        List<PaisDisponibleResponse> paises = proveedorPaises.obtenerPaises().stream()
                .sorted(Comparator.comparing(PaisFuente::nombre, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new PaisDisponibleResponse(
                        p.isoAlpha2(), p.nombre(), p.continente(), p.code(), p.href(), p.mapeado()))
                .toList();
        return ResponseEntity.ok(paises);
    }

    private PaisResponse toPaisResponse(Pais pais) {
        return new PaisResponse(
                pais.id(), pais.nombre(), pais.isoAlpha2(), pais.continente(),
                pais.code(), pais.href(), pais.mapeado());
    }
}