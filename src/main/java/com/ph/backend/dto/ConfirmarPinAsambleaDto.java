package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarPinAsambleaDto {
    private Long asambleaId;
    private Long unidadPrivadaId;
    private String cedulaPersona;
    private String pin;
}
