package com.ph.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final RestClient restClient = RestClient.create();

    @Async
    public boolean enviarClaveAcceso(String emailDestino, String nombreCompleto, String claveOtp) {
        String htmlBody = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #0f172a; color: #f8fafc;">
                <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #1e293b;">
                    <h2 style="color: #6366f1; margin: 0;">proyectoPH — Asamblea General</h2>
                    <p style="color: #94a3b8; font-size: 14px; margin-top: 5px;">Plataforma de Votaciones Ley 675 de 2001</p>
                </div>
                
                <div style="padding: 20px 0;">
                    <p style="font-size: 16px; color: #cbd5e1;">Hola <strong>%s</strong>,</p>
                    <p style="font-size: 14px; color: #94a3b8;">Tu solicitud de ingreso presencial/virtual a la asamblea ha sido procesada. Utiliza el siguiente código de verificación temporal de 6 dígitos para acceder:</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #10b981; background-color: #022c22; padding: 12px 24px; border-radius: 8px; border: 1px solid #059669;">
                            %s
                        </span>
                    </div>
                    
                    <p style="font-size: 12px; color: #64748b; text-align: center;">Este código es de uso personal e intransferible. Válido durante la sesión de la asamblea.</p>
                </div>
                
                <div style="text-align: center; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b;">
                    <p>proyectoPH &copy; 2026 — Gestión de Propiedad Horizontal</p>
                </div>
            </div>
            """, nombreCompleto, claveOtp);

        Map<String, Object> body = Map.of(
            "from", fromEmail,
            "to", List.of(emailDestino),
            "subject", "🔑 Tu Clave de Acceso a la Asamblea — proyectoPH",
            "html", htmlBody
        );

        try {
            String response = restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return true;
        } catch (Exception ex) {
            log.error("[BACKEND EMAIL] ❌ Error al enviar correo vía Resend API: {}", ex.getMessage(), ex);
            return false;
        }
    }

    @Async
    public boolean enviarCredencialesIniciales(String emailDestino, String nombreCompleto, String username, String passwordTemporal, String nombreConjunto) {
        String conjuntoInfo = (nombreConjunto != null && !nombreConjunto.isBlank()) 
                ? "<p style=\"font-size: 14px; color: #94a3b8;\">Conjunto Residencial: <strong>" + nombreConjunto + "</strong></p>" : "";

        String htmlBody = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #0f172a; color: #f8fafc;">
                <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #1e293b;">
                    <h2 style="color: #6366f1; margin: 0;">proyectoPH Multi-Tenant</h2>
                    <p style="color: #94a3b8; font-size: 14px; margin-top: 5px;">Bienvenido a la Plataforma de Propiedad Horizontal</p>
                </div>
                
                <div style="padding: 20px 0;">
                    <p style="font-size: 16px; color: #cbd5e1;">Hola <strong>%s</strong>,</p>
                    <p style="font-size: 14px; color: #94a3b8;">Se ha creado tu cuenta de acceso a la plataforma. A continuación encontrarás tus credenciales de ingreso iniciales:</p>
                    
                    %s
                    
                    <div style="background-color: #1e293b; padding: 16px; border-radius: 8px; margin: 20px 0; border: 1px solid #334155;">
                        <p style="margin: 6px 0; font-size: 14px; color: #cbd5e1;">👤 <strong>Nombre de Usuario:</strong> <code style="color: #f59e0b; font-size: 15px;">%s</code></p>
                        <p style="margin: 6px 0; font-size: 14px; color: #cbd5e1;">🔐 <strong>Contraseña Temporal:</strong> <code style="color: #10b981; font-size: 15px;">%s</code></p>
                    </div>
                    
                    <div style="background-color: #451a03; border: 1px solid #7c2d12; border-radius: 8px; padding: 12px; margin-top: 15px;">
                        <p style="margin: 0; font-size: 13px; color: #fdba74; text-align: center;">
                            ⚠️ <strong>Requisito de Seguridad:</strong> Al ingresar por primera vez, el sistema te solicitará cambiar esta contraseña temporal por tu nueva contraseña personal permanente.
                        </p>
                    </div>
                </div>
                
                <div style="text-align: center; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b;">
                    <p>proyectoPH &copy; 2026 — Gestión de Propiedad Horizontal</p>
                </div>
            </div>
            """, nombreCompleto, conjuntoInfo, username, passwordTemporal);

        Map<String, Object> body = Map.of(
            "from", fromEmail,
            "to", List.of(emailDestino),
            "subject", "🎉 Tus Credenciales de Acceso a proyectoPH",
            "html", htmlBody
        );

        try {
            String response = restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return true;
        } catch (Exception ex) {
            log.error("[BACKEND EMAIL] ❌ Error al enviar credenciales iniciales vía Resend API: {}", ex.getMessage(), ex);
            return false;
        }
    }

    @Async
    public boolean enviarCorreoHtml(String emailDestino, String asunto, String cuerpoHtml) {
        Map<String, Object> body = Map.of(
            "from", fromEmail,
            "to", List.of(emailDestino),
            "subject", asunto,
            "html", cuerpoHtml
        );

        try {
            String response = restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return true;
        } catch (Exception ex) {
            log.error("[BACKEND EMAIL] ❌ Error enviando correo HTML vía Resend API a {}: {}", emailDestino, ex.getMessage(), ex);
            return false;
        }
    }
}
