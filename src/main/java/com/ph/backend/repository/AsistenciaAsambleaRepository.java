package com.ph.backend.repository;

import com.ph.backend.model.AsistenciaAsamblea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaAsambleaRepository extends JpaRepository<AsistenciaAsamblea, Long> {
    List<AsistenciaAsamblea> findByAsambleaId(Long asambleaId);
    Page<AsistenciaAsamblea> findByAsambleaId(Long asambleaId, Pageable pageable);
    Optional<AsistenciaAsamblea> findByAsambleaIdAndUnidadPrivadaId(Long asambleaId, Long unidadPrivadaId);
    
    @Query("SELECT COALESCE(SUM(a.unidadPrivada.coeficiente), 0.0) FROM AsistenciaAsamblea a WHERE a.asamblea.id = :asambleaId")
    Double sumCoeficienteByAsambleaId(@Param("asambleaId") Long asambleaId);

    @Query("SELECT COUNT(a) FROM AsistenciaAsamblea a WHERE a.asamblea.id = :asambleaId")
    Long countAsistentesByAsambleaId(@Param("asambleaId") Long asambleaId);

    @Query("SELECT a.asamblea.id, COUNT(a) FROM AsistenciaAsamblea a WHERE a.asamblea.id IN :asambleaIds GROUP BY a.asamblea.id")
    List<Object[]> countAsistentesByAsambleaIdsIn(@Param("asambleaIds") List<Long> asambleaIds);

    boolean existsByAsambleaIdAndUnidadPrivadaId(Long asambleaId, Long unidadPrivadaId);
}
