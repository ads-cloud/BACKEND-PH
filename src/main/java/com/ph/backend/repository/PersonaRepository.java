package com.ph.backend.repository;

import com.ph.backend.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    @Query("SELECT p FROM Persona p WHERE p.cedula = :cedula ORDER BY p.id ASC")
    List<Persona> findAllByCedula(@Param("cedula") String cedula);

    default Optional<Persona> findByCedula(String cedula) {
        List<Persona> list = findAllByCedula(cedula);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT p FROM Persona p WHERE p.email = :email ORDER BY p.id ASC")
    List<Persona> findAllByEmail(@Param("email") String email);

    default Optional<Persona> findByEmail(String email) {
        List<Persona> list = findAllByEmail(email);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
