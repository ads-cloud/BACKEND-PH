package com.ph.backend.repository;

import com.ph.backend.model.PlantillaEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaEmailRepository extends JpaRepository<PlantillaEmail, Long> {

    Optional<PlantillaEmail> findByCodigoAndCopropiedadId(String codigo, Long copropiedadId);

    Optional<PlantillaEmail> findByCodigoAndCopropiedadIdIsNull(String codigo);

    @Query("SELECT p FROM PlantillaEmail p WHERE p.codigo = :codigo AND (p.copropiedadId = :copropiedadId OR p.copropiedadId IS NULL) ORDER BY p.copropiedadId DESC NULLS LAST")
    List<PlantillaEmail> findBestMatch(@Param("codigo") String codigo, @Param("copropiedadId") Long copropiedadId);

    List<PlantillaEmail> findByCopropiedadId(Long copropiedadId);

    List<PlantillaEmail> findByCopropiedadIdIsNull();
}
