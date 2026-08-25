package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private Long personaId;
    private Long unidadPrivadaId;
    private Long copropiedadId;
    private String copropiedadNombre;
    private String nombreCompleto;
    private String torre;
    private String numeroUnidad;
    private Double coeficiente;
    private Long asambleaActivaId;
    private String tituloAsamblea;
    private Boolean asistenciaRegistrada;
    private String token;
}
