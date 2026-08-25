package com.ph.backend.repository;

import com.ph.backend.model.VotoEmitido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotoEmitidoRepository extends JpaRepository<VotoEmitido, Long> {
    List<VotoEmitido> findByPreguntaId(Long preguntaId);
    Optional<VotoEmitido> findByPreguntaIdAndUnidadPrivadaId(Long preguntaId, Long unidadPrivadaId);
    boolean existsByPreguntaIdAndUnidadPrivadaId(Long preguntaId, Long unidadPrivadaId);
    void deleteByPreguntaId(Long preguntaId);

    @Query("SELECT COALESCE(SUM(v.coeficienteAplicado), 0.0) FROM VotoEmitido v WHERE v.opcionVoto.id = :opcionId")
    Double sumCoeficienteByOpcionId(@Param("opcionId") Long opcionId);

    @Query("SELECT COUNT(v) FROM VotoEmitido v WHERE v.opcionVoto.id = :opcionId")
    Long countVotosByOpcionId(@Param("opcionId") Long opcionId);

    @Query("SELECT COALESCE(SUM(v.coeficienteAplicado), 0.0) FROM VotoEmitido v WHERE v.pregunta.id = :preguntaId")
    Double sumTotalCoeficienteByPreguntaId(@Param("preguntaId") Long preguntaId);

    Long countByPreguntaId(Long preguntaId);

    @Query("SELECT COUNT(v) FROM VotoEmitido v WHERE v.pregunta.asamblea.copropiedad.id = :copropiedadId")
    Long countByCopropiedadId(@Param("copropiedadId") Long copropiedadId);
}
