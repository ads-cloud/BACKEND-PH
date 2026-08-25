package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitanteRequestDTO {

    private String cedula;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private Boolean esPropietarioPrincipal;
    private Boolean esArrendatario;
    private Boolean esApoderadoDesignado;
    private String parentesco;

    @JsonProperty("confirmarCambioPropietario")
    private Boolean confirmarCambioPropietario;

    @JsonProperty("confirmarCambioApoderado")
    private Boolean confirmarCambioApoderado;

    @JsonProperty("confirmarCambio")
    private Boolean confirmarCambio;
}
