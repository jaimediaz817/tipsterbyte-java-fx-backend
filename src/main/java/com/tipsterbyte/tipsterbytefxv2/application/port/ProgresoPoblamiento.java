// ─────────────────────────────────────────────
// [QUÉ]: Puerto de progreso del poblamiento geográfico (FASE T3): permite a CU-10
//        reportar en qué país va sin conocer el mecanismo de publicación.
// [POR QUÉ]: El poblamiento asíncrono necesita exponer "paisActual / paisesProcesados"
//            para el polling del frontend. CU-10 notifica vía este puerto; la
//            implementación en memoria mantiene el snapshot que lee el endpoint de
//            estado. Nullable en CU-10: si no hay consumidor, el poblamiento corre igual
//            (compatibilidad con tests y uso síncrono).
// [ALTERNATIVAS]: Callbacks por lambda en el constructor; se descarta porque acopla
//                 CU-10 a un consumer concreto y complica el wiring.
// [RELACIONES]: Notificado por SincronizarCatalogoUseCase (CU-10); implementado por
//               ProgresoPoblamientoEnMemoria; leído por CatalogoController (FASE T3).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import java.util.Optional;

public interface ProgresoPoblamiento {

    // [QUÉ]: Reporta avance: país en curso y cuántos países completados van.
    void actualizar(String paisEnCurso, int paisesProcesados);

    // [QUÉ]: Snapshot actual para lectura del endpoint de estado; vacío si nunca inició.
    Optional<Progreso> snapshot();

    // [QUÉ]: Reinicia el snapshot al comenzar una nueva ejecución.
    void reiniciar();

    record Progreso(String paisActual, int paisesProcesados) {
    }
}
