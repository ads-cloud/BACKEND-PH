package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaRequestDto {
    private Long asambleaId;
    private Long unidadPrivadaId;
    private String cedulaPersona;
    @Builder.Default
    private Boolean esApoderado = false;
}
