// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-03 (HU-03): sincroniza las cuotas de los partidos próximos
//        de una liga desde la fuente de odds.
// [POR QUÉ]: Orquesta la actualización de cuotas por partido, delegando la validación
//            de BR-007 (cuota > 1.0) al VO Cuota. Emite CuotaActualizada por partido.
// [ALTERNATIVAS]: Obtener cuotas de todos los partidos históricos; se descarta porque
//                 solo los próximos (PROGRAMADO/EN_VIVO) requieren odds vigentes.
// [RELACIONES]: HU-03 → CU-03 → ProveedorCuotas + PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SincronizarCuotasUseCase {

    private final PartidoRepository partidoRepository;
    private final ProveedorCuotas proveedorCuotas;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public SincronizarCuotasUseCase(PartidoRepository partidoRepository, ProveedorCuotas proveedorCuotas) {
        this.partidoRepository = partidoRepository;
        this.proveedorCuotas = proveedorCuotas;
    }

    // [QUÉ]: Ejecuta CU-03: para cada partido próximo de la liga, consulta cuotas, las
    //        mapea a Cuota y actualiza el partido. Devuelve los eventos CuotaActualizada.
    public List<DomainEvent> ejecutar(UUID ligaId) {
        List<Partido> partidos = partidoRepository.buscarProximosPorLiga(ligaId);

        List<DomainEvent> eventos = new ArrayList<>();
        for (Partido partido : partidos) {
            List<CuotaFuente> fuentes = proveedorCuotas.obtenerCuotas(partido.id());
            List<Cuota> cuotas = fuentes.stream()
                    .map(f -> new Cuota(f.valor())) // BR-007 validado en el VO
                    .toList();
            partido.actualizarCuotas(cuotas);
            partidoRepository.guardar(partido);
            eventos.addAll(partido.pullEventos()); // CuotaActualizada
        }
        return eventos;
    }
}