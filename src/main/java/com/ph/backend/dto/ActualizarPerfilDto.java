package com.ph.backend.dto;

import lombok.Data;

@Data
public class ActualizarPerfilDto {
    private String documento;
    private String email;
    private String telefono;
    private String nuevaPassword;
}
