package com.ph.backend.repository;

import com.ph.backend.model.Pregunta;
import com.ph.backend.model.PreguntaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByAsambleaIdOrderByIdDesc(Long asambleaId);
    Optional<Pregunta> findFirstByAsambleaIdAndEstado(Long asambleaId, PreguntaStatus estado);
    boolean existsByAsambleaIdAndEstadoIn(Long asambleaId, List<PreguntaStatus> estados);
}
