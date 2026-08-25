package com.ph.backend.repository;

import com.ph.backend.model.Modulo;
import com.ph.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    Optional<Modulo> findByCodigo(String codigo);

    List<Modulo> findByActivoTrueOrderByOrdenAsc();

    long countByActivoTrue();

    List<Modulo> findAllByOrderByOrdenAsc();

    @Query("SELECT DISTINCT m FROM Modulo m JOIN m.rolesPermitidos r WHERE m.activo = true AND r IN :roles ORDER BY m.orden ASC")
    List<Modulo> findByRolesPermitidosInAndActivoTrueOrderByOrdenAsc(@Param("roles") Collection<Rol> roles);
}
