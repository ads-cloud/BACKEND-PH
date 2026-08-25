package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoCopropietarioDto {
    private String cedula;
    private String torre;
    private String numeroUnidad;
}
