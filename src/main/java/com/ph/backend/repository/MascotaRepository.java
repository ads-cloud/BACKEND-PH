package com.ph.backend.repository;

import com.ph.backend.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    List<Mascota> findByUnidadId(Long unidadId);
    void deleteByUnidadId(Long unidadId);
}
