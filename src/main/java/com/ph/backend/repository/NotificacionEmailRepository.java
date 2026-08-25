package com.ph.backend.repository;

import com.ph.backend.model.EstadoNotificacion;
import com.ph.backend.model.NotificacionEmail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionEmailRepository extends JpaRepository<NotificacionEmail, Long> {
    List<NotificacionEmail> findByEstadoAndIntentosLessThanOrderByFechaCreacionAsc(EstadoNotificacion estado, int maxIntentos, Pageable pageable);
    List<NotificacionEmail> findByCopropiedadId(Long copropiedadId);
    long countByEstado(EstadoNotificacion estado);
}
