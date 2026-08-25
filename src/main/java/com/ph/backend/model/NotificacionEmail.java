package com.ph.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_email", indexes = {
    @Index(name = "idx_notif_estado_copropiedad", columnList = "estado, copropiedad_id"),
    @Index(name = "idx_notif_persona_id", columnList = "persona_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "copropiedad_id")
    private Long copropiedadId;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "email_destino", nullable = false)
    private String emailDestino;

    @Column(name = "nombre_destinatario")
    private String nombreDestinatario;

    private String asunto;

    @Column(name = "cuerpo_html", columnDefinition = "TEXT")
    private String cuerpoHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacion", nullable = false)
    private TipoNotificacion tipoNotificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
