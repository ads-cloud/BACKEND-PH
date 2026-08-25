package com.ph.backend.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopropiedadResponseDto {
    private Long id;
    private String nombre;
    private String nit;
    private String direccion;
    private Double totalCoeficiente;
    private Integer totalUnidades;
    private Integer totalModulosPermitidos;
    private List<AdminInfoDto> administradores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminInfoDto {
        private Long id;
        private String documento;
        private String username;
        private String nombreCompleto;
        private String email;
        private String telefono;
    }
}
