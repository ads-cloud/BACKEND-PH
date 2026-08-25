package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidadRepresentadaDto {
    private Long unidadId;
    private String torre;
    private String numeroUnidad;
    private Double coeficiente;
    private Boolean esPorPoder; // true si es apoderado, false si es propietario directo
    private Boolean poderAprobado; // true si el administrador aprobó el poder
    private Boolean asistenciaRegistrada; // true si el administrador registró la asistencia presencial en la entrada
    private Boolean asistenciaConfirmada; // true si ya ingresó el PIN correcto de la asamblea
}
