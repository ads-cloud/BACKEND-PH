package com.ph.backend.service;

import com.ph.backend.model.ConfigTarea;
import com.ph.backend.model.EstadoNotificacion;
import com.ph.backend.model.NotificacionEmail;
import com.ph.backend.repository.ConfigTareaRepository;
import com.ph.backend.repository.NotificacionEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionTaskScheduler {

    private final NotificacionEmailRepository notificacionEmailRepository;
    private final ConfigTareaRepository configTareaRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void procesarColaNotificaciones() {
        ConfigTarea config = configTareaRepository.findByNombreTarea("ENVIO_CORREOS_AUTOMATICO")
                .orElse(null);

        if (config != null && Boolean.FALSE.equals(config.getActiva())) {
            log.trace("[TASK SCHEDULER] ⏸️ Tarea 'ENVIO_CORREOS_AUTOMATICO' desactivada en config_tareas.");
            return;
        }

        List<NotificacionEmail> pendientes = notificacionEmailRepository
                .findByEstadoAndIntentosLessThanOrderByFechaCreacionAsc(EstadoNotificacion.PENDIENTE, 3, PageRequest.of(0, 10));

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[TASK SCHEDULER] ⚙️ Procesando lote de {} notificaciones pendientes...", pendientes.size());

        for (NotificacionEmail notif : pendientes) {
            notif.setIntentos(notif.getIntentos() + 1);
            try {
                emailService.enviarCorreoHtml(notif.getEmailDestino(), notif.getAsunto(), notif.getCuerpoHtml());
                notif.setEstado(EstadoNotificacion.ENVIADO);
                notif.setFechaEnvio(LocalDateTime.now());
                log.info("[TASK SCHEDULER] ✅ Correo ID {} enviado a {} (PH ID: {})", notif.getId(), notif.getEmailDestino(), notif.getCopropiedadId());
            } catch (Exception ex) {
                log.error("[TASK SCHEDULER] ❌ Error enviando correo ID {}: {}", notif.getId(), ex.getMessage());
                if (notif.getIntentos() >= 3) {
                    notif.setEstado(EstadoNotificacion.FALLIDO);
                }
                notif.setErrorLog(ex.getMessage());
            }
            notificacionEmailRepository.save(notif);
        }
    }
}
