// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia para el historial de cuotas append-only (HU-14 AC4.5).
// [POR QUÉ]: Cada sincronización registra las cuotas observadas; HU-15 las consulta
//            para calcular volatilidad y mostrar series. El dominio no conoce JPA.
// [RELACIONES]: Implementado por CuotaHistorialRepositoryJpaAdapter (infrastructure).
//               Consumido por SincronizarCuotasUseCase (escritura) + HU-15 (lectura).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;

import java.util.List;
import java.util.UUID;

public interface CuotaHistorialRepository {

    // [QUÉ]: Registra una cuota observada (escritura incondicional, sin deduplicación).
    void guardar(CuotaHistorial cuota);

    // [QUÉ]: Registra un lote de cuotas observadas (batch write para una corrida).
    void guardarLote(List<CuotaHistorial> cuotas);

    // [QUÉ]: Devuelve las cuotas de un partido en un rango de tiempo (para HU-15).
    List<CuotaHistorial> buscarPorPartidoYRango(UUID partidoId, java.time.Instant desde, java.time.Instant hasta);
}
