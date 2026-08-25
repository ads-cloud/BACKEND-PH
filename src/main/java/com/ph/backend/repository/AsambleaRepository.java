package com.ph.backend.repository;

import com.ph.backend.model.Asamblea;
import com.ph.backend.model.AsambleaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsambleaRepository extends JpaRepository<Asamblea, Long> {
    List<Asamblea> findByCopropiedadId(Long copropiedadId);
    Optional<Asamblea> findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(Long copropiedadId, AsambleaStatus estado);
    List<Asamblea> findByEstado(AsambleaStatus estado);
    boolean existsByCopropiedadIdAndEstado(Long copropiedadId, AsambleaStatus estado);
    boolean existsByCopropiedadIdAndEstadoIn(Long copropiedadId, List<AsambleaStatus> estados);
    List<Asamblea> findByCopropiedadIdInAndEstadoIn(List<Long> copropiedadIds, List<AsambleaStatus> estados);
}
