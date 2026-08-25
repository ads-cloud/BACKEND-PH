package com.ph.backend.service;

import com.ph.backend.dto.RenderedEmailDto;
import com.ph.backend.model.EstadoNotificacion;
import com.ph.backend.model.NotificacionEmail;
import com.ph.backend.model.TipoNotificacion;
import com.ph.backend.repository.NotificacionEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionEmailRepository notificacionEmailRepository;
    private final PlantillaEmailService plantillaEmailService;

    @Transactional
    public NotificacionEmail encolarCredencialesIniciales(Long copropiedadId, String email, String nombre, String username, String password, String contexto) {
        if (email == null || email.isBlank() || email.endsWith("@proyectoph.com")) {
            return null;
        }

        Map<String, String> variables = Map.of(
                "nombre", nombre != null ? nombre : "Usuario",
                "username", username != null ? username : "",
                "password", password != null ? password : "",
                "contexto", contexto != null ? contexto : "Acceso a la Plataforma"
        );

        RenderedEmailDto rendered = plantillaEmailService.procesarPlantilla("CREDENCIALES_INICIALES", copropiedadId, variables);

        NotificacionEmail notificacion = NotificacionEmail.builder()
                .copropiedadId(copropiedadId)
                .emailDestino(email.trim())
                .nombreDestinatario(nombre)
                .asunto(rendered.getAsunto())
                .cuerpoHtml(rendered.getCuerpoHtml())
                .tipoNotificacion(TipoNotificacion.CREDENCIALES_INICIALES)
                .estado(EstadoNotificacion.PENDIENTE)
                .intentos(0)
                .build();

        NotificacionEmail guardada = notificacionEmailRepository.save(notificacion);
        return guardada;
    }

    @Transactional
    public NotificacionEmail encolarPinAsamblea(Long copropiedadId, String email, String nombre, String pinAsamblea) {
        if (email == null || email.isBlank() || email.endsWith("@proyectoph.com")) {
            return null;
        }

        Map<String, String> variables = Map.of(
                "nombre", nombre != null ? nombre : "Usuario",
                "pinAsamblea", pinAsamblea != null ? pinAsamblea : ""
        );

        RenderedEmailDto rendered = plantillaEmailService.procesarPlantilla("OTP_ASAMBLEA", copropiedadId, variables);

        NotificacionEmail notificacion = NotificacionEmail.builder()
                .copropiedadId(copropiedadId)
                .emailDestino(email.trim())
                .nombreDestinatario(nombre)
                .asunto(rendered.getAsunto())
                .cuerpoHtml(rendered.getCuerpoHtml())
                .tipoNotificacion(TipoNotificacion.OTP)
                .estado(EstadoNotificacion.PENDIENTE)
                .intentos(0)
                .build();

        NotificacionEmail guardada = notificacionEmailRepository.save(notificacion);
        return guardada;
    }

    @Transactional
    public NotificacionEmail registrarHistoricoNotificacion(Long copropiedadId, Long personaId, String email, String nombre, String asunto, String htmlBody, TipoNotificacion tipo, boolean fueEnviadoExitosamente) {
        String destEmail = (email != null && !email.isBlank()) ? email.trim() : "SIN_CORREO";

        NotificacionEmail notificacion = NotificacionEmail.builder()
                .copropiedadId(copropiedadId)
                .personaId(personaId)
                .emailDestino(destEmail)
                .nombreDestinatario(nombre)
                .asunto(asunto)
                .cuerpoHtml(htmlBody)
                .tipoNotificacion(tipo != null ? tipo : TipoNotificacion.RESET_PASSWORD)
                .estado(fueEnviadoExitosamente ? EstadoNotificacion.ENVIADO : EstadoNotificacion.FALLIDO)
                .intentos(1)
                .fechaEnvio(fueEnviadoExitosamente ? java.time.LocalDateTime.now() : null)
                .errorLog(fueEnviadoExitosamente ? null : (htmlBody != null && htmlBody.contains("FALLA:") ? htmlBody : "Error o proveedor no disponible en envío directo"))
                .build();

        NotificacionEmail guardada = notificacionEmailRepository.save(notificacion);
        return guardada;
    }
}
