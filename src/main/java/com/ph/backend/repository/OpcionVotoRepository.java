package com.ph.backend.repository;

import com.ph.backend.model.OpcionVoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcionVotoRepository extends JpaRepository<OpcionVoto, Long> {
    List<OpcionVoto> findByPreguntaIdOrderByOrdenAsc(Long preguntaId);
}
