package com.ph.backend.repository;

import com.ph.backend.model.PuntoOrdenDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuntoOrdenDiaRepository extends JpaRepository<PuntoOrdenDia, Long> {
    List<PuntoOrdenDia> findByAsambleaIdOrderByOrdenAsc(Long asambleaId);
    long countByAsambleaIdAndCompletadoFalse(Long asambleaId);
    long countByAsambleaId(Long asambleaId);
    void deleteByAsambleaId(Long asambleaId);
}
