package com.ph.backend.repository;

import com.ph.backend.model.ConfigTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigTareaRepository extends JpaRepository<ConfigTarea, Long> {
    Optional<ConfigTarea> findByNombreTarea(String nombreTarea);
}
