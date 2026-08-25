package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarUnidadDto {
    private String torre;
    private String numeroUnidad;
    private Double coeficiente;
    private String cedulaPropietario;
    private String nombrePropietario;
    private String emailPropietario;
    private String telefonoPropietario;
    private String documentoApoderado;
    private String nombreApoderado;
    private String emailApoderado;
    private String telefonoApoderado;
}
