package com.ph.backend.repository;

import com.ph.backend.model.Copropiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CopropiedadRepository extends JpaRepository<Copropiedad, Long> {
    Optional<Copropiedad> findByNit(String nit);
}
