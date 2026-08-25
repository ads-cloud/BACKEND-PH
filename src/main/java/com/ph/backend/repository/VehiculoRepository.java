package com.ph.backend.repository;

import com.ph.backend.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByUnidadId(Long unidadId);
    void deleteByUnidadId(Long unidadId);
}
