package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TareaProgramadaRepository extends JpaRepository<TareaProgramada, UUID> {
    // Custom finder methods used by the scheduler and the service layer
    Optional<TareaProgramada> buscarPorIsoAlpha2(String isoAlpha2);
    Iterable<TareaProgramada> listarPorPrioridadAsc();
    void eliminarPorIsoAlpha2(String isoAlpha2);
}