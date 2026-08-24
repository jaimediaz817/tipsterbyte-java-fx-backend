// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-17: sincroniza solo el catálogo de países desde la fuente #1.
// [POR QUÉ]: Paso 1 del poblamiento granular HU-12. Es síncrono porque #1 devuelve
//            ~176 filas sin scraping pesado (<2s) y no justifica async/polling. Es
//            idempotente (clave natural isoAlpha2) y deja el estado listo para el paso 2
//            (poblar ligas por país). Invalida el cache de países para que
//            GET /paises/disponibles vea datos frescos.
// [ALTERNATIVAS]: Reutilizar CU-10 completo; se descarta porque obligaría a recorrer
//                 176 países y sus ligas solo para refrescar países.
// [RELACIONES]: HU-12 → CU-17 → ProveedorPaises (#1) + PaisRepository + CacheLecturas.
//               Consumido por CatalogoController POST /poblar-paises (200).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;

import java.util.List;
import java.util.UUID;

public final class SincronizarPaisesUseCase {

    public record ResultadoPaises(int totalPaises, int nuevos) {}

    private final ProveedorPaises proveedorPaises;
    private final PaisRepository paisRepository;
    private final CacheLecturas cacheLecturas;

    public SincronizarPaisesUseCase(ProveedorPaises proveedorPaises,
                                    PaisRepository paisRepository,
                                    CacheLecturas cacheLecturas) {
        this.proveedorPaises = proveedorPaises;
        this.paisRepository = paisRepository;
        this.cacheLecturas = cacheLecturas;
    }

    // [QUÉ]: Ejecuta CU-17: obtiene países de #1 y persiste los nuevos.
    public ResultadoPaises ejecutar() {
        cacheLecturas.eliminar(CacheClaves.paises());
        List<PaisFuente> paisesFuente = proveedorPaises.obtenerPaises();
        int nuevos = 0;
        for (PaisFuente fuente : paisesFuente) {
            boolean existe = paisRepository.buscarPorIsoAlpha2(fuente.isoAlpha2()).isPresent();
            if (!existe) {
                Pais nuevo = new Pais(
                        UUID.randomUUID(),
                        fuente.nombre(), fuente.isoAlpha2(), fuente.continente(),
                        fuente.code(), fuente.href(), fuente.mapeado());
                paisRepository.guardar(nuevo);
                nuevos++;
            }
        }
        int total = paisRepository.buscarTodos().size();
        return new ResultadoPaises(total, nuevos);
    }
}
