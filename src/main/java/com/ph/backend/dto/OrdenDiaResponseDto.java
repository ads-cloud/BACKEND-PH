package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDiaResponseDto {
    private Long asambleaId;
    private Boolean ordenDiaDefinitivo;
    private List<PuntoOrdenDiaDto> puntos;
}
