package com.ph.backend.repository;

import com.ph.backend.model.UnidadHabitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadHabitanteRepository extends JpaRepository<UnidadHabitante, Long> {

    List<UnidadHabitante> findByUnidadId(Long unidadId);

    List<UnidadHabitante> findAllByUnidadIdAndPersonaId(Long unidadId, Long personaId);

    default Optional<UnidadHabitante> findByUnidadIdAndPersonaId(Long unidadId, Long personaId) {
        List<UnidadHabitante> list = findAllByUnidadIdAndPersonaId(unidadId, personaId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<UnidadHabitante> findAllByUnidadIdAndPersonaCedula(Long unidadId, String cedula);

    default Optional<UnidadHabitante> findByUnidadIdAndPersonaCedula(Long unidadId, String cedula) {
        List<UnidadHabitante> list = findAllByUnidadIdAndPersonaCedula(unidadId, cedula);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<UnidadHabitante> findAllByUnidadIdAndEsPropietarioPrincipalTrue(Long unidadId);

    default Optional<UnidadHabitante> findByUnidadIdAndEsPropietarioPrincipalTrue(Long unidadId) {
        List<UnidadHabitante> list = findAllByUnidadIdAndEsPropietarioPrincipalTrue(unidadId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<UnidadHabitante> findByUnidadIdAndEsApoderadoDesignadoTrue(Long unidadId);

    void deleteByUnidadIdAndPersonaId(Long unidadId, Long personaId);
}
