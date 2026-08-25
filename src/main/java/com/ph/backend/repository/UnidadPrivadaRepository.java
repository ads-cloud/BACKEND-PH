package com.ph.backend.repository;

import com.ph.backend.model.UnidadPrivada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadPrivadaRepository extends JpaRepository<UnidadPrivada, Long> {
    List<UnidadPrivada> findByCopropiedadId(Long copropiedadId);
    Page<UnidadPrivada> findByCopropiedadId(Long copropiedadId, Pageable pageable);
    Long countByCopropiedadId(Long copropiedadId);
    
    @Query("SELECT u.copropiedad.id, COUNT(u) FROM UnidadPrivada u WHERE u.copropiedad.id IN :copropiedadIds GROUP BY u.copropiedad.id")
    List<Object[]> countByCopropiedadIdsIn(@Param("copropiedadIds") List<Long> copropiedadIds);
    
    Optional<UnidadPrivada> findByCopropiedadIdAndTorreAndNumeroUnidad(Long copropiedadId, String torre, String numeroUnidad);
    Optional<UnidadPrivada> findByTorreAndNumeroUnidad(String torre, String numeroUnidad);

    @Query("SELECT u FROM UnidadPrivada u LEFT JOIN u.propietario p LEFT JOIN u.apoderado a LEFT JOIN a.persona ap WHERE (p IS NOT NULL AND p.cedula = :documento) OR (ap IS NOT NULL AND ap.cedula = :documento)")
    List<UnidadPrivada> findByPropietarioCedulaOrApoderadoDocumento(@Param("documento") String documento);

    @Query("SELECT DISTINCT u FROM UnidadPrivada u " +
           "LEFT JOIN u.propietario p " +
           "LEFT JOIN u.apoderado a LEFT JOIN a.persona ap " +
           "WHERE (p IS NOT NULL AND p.cedula = :documento) " +
           "   OR (ap IS NOT NULL AND ap.cedula = :documento) " +
           "   OR u.id IN (SELECT h.unidad.id FROM UnidadHabitante h WHERE h.persona.cedula = :documento)")
    List<UnidadPrivada> findMisInmueblesPorDocumento(@Param("documento") String documento);
}

