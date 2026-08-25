package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {
    private String status;
    private String database;
    private String rabbitmq;
    private LocalDateTime timestamp;

    // Sección de Monitoreo de Notificaciones Email
    private NotificacionesMonitoreoDto notificacionesEmail;

    // Sección de Monitoreo de Colas RabbitMQ
    private List<RabbitColaMonitoreoDto> colasRabbit;

    // Sección de Monitoreo por Propiedad Horizontal
    private List<CopropiedadMonitoreoDto> copropiedadesMonitoreo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificacionesMonitoreoDto {
        private Long pendientes;
        private Long enviados;
        private Long fallidos;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RabbitColaMonitoreoDto {
        private String nombreCola;
        private Integer mensajesPendientes;
        private Integer consumidoresActivos;
        private String estado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopropiedadMonitoreoDto {
        private Long copropiedadId;
        private String copropiedadNombre;
        private String nit;
        private Long unidadesRegistradas; // Cantidad de unidades registradas para el PH
        private Long usuariosActivos;      // Cantidad de usuarios de inicio de sesión activos

        // Información de Asambleas Activas / En Curso
        private Boolean tieneAsambleaActiva;
        private Long asambleaActivaId;
        private String asambleaTitulo;
        private String asambleaEstado; // 'EN_CURSO', 'CONVOCADA', etc.
        private Long asistentesQuorum;  // Cantidad de unidades registradas en quórum para la asamblea activa
        private Long votosRegistrados;  // Cantidad total de votos procesados/ingresados en la asamblea activa
    }
}
