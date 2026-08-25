package com.ph.backend.repository;

import com.ph.backend.model.CopropiedadModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopropiedadModuloRepository extends JpaRepository<CopropiedadModulo, Long> {

    List<CopropiedadModulo> findByCopropiedadId(Long copropiedadId);

    List<CopropiedadModulo> findByCopropiedadIdAndActivoTrue(Long copropiedadId);
    
    List<CopropiedadModulo> findByCopropiedadIdInAndActivoTrue(List<Long> copropiedadIds);

    List<CopropiedadModulo> findByCopropiedadIdAndActivoFalse(Long copropiedadId);

    Optional<CopropiedadModulo> findByCopropiedadIdAndModuloId(Long copropiedadId, Long moduloId);
}
