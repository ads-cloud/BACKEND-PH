package com.ph.backend.repository;

import com.ph.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE u.persona.cedula = :documento ORDER BY u.id ASC")
    List<Usuario> findAllByDocumento(@Param("documento") String documento);

    default Optional<Usuario> findByDocumento(String documento) {
        List<Usuario> list = findAllByDocumento(documento);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT u FROM Usuario u WHERE u.persona.email = :email ORDER BY u.id ASC")
    List<Usuario> findAllByEmail(@Param("email") String email);

    default Optional<Usuario> findByEmail(String email) {
        List<Usuario> list = findAllByEmail(email);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<Usuario> findAllByUsername(String username);

    default Optional<Usuario> findByUsername(String username) {
        List<Usuario> list = findAllByUsername(username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.persona.cedula = :documento")
    boolean existsByDocumento(@Param("documento") String documento);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.copropiedadesAsignadas c JOIN u.roles r WHERE c.id = :copropiedadId AND r = com.ph.backend.model.Rol.ROLE_GESTOR")
    List<Usuario> findGestoresByCopropiedadId(@Param("copropiedadId") Long copropiedadId);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.copropiedadesAsignadas c JOIN u.roles r WHERE c.id = :copropiedadId AND r = com.ph.backend.model.Rol.ROLE_ADMIN")
    List<Usuario> findAdminsByCopropiedadId(@Param("copropiedadId") Long copropiedadId);

    @Query("SELECT c.id, u FROM Usuario u JOIN FETCH u.persona p JOIN u.copropiedadesAsignadas c JOIN u.roles r WHERE c.id IN :copropiedadIds AND r = com.ph.backend.model.Rol.ROLE_ADMIN")
    List<Object[]> findAdminsByCopropiedadIdsIn(@Param("copropiedadIds") List<Long> copropiedadIds);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.copropiedadesAsignadas c JOIN u.roles r WHERE c.id = :copropiedadId AND r = com.ph.backend.model.Rol.ROLE_APODERADO")
    List<Usuario> findApoderadosByCopropiedadId(@Param("copropiedadId") Long copropiedadId);

    @Query("SELECT c.id, COUNT(u) FROM Usuario u JOIN u.copropiedadesAsignadas c WHERE c.id IN :copropiedadIds AND (u.activo IS NULL OR u.activo = true) GROUP BY c.id")
    List<Object[]> countUsuariosActivosByCopropiedadIdsIn(@Param("copropiedadIds") List<Long> copropiedadIds);
}
