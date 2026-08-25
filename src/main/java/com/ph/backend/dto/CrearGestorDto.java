package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearGestorDto {
    private String cedula;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private Long copropiedadId;
}
