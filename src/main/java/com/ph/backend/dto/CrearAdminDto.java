package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAdminDto {
    private String documento;
    private String nombreCompleto;
    private String email;
    private String telefono;
}
