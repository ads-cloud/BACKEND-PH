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
public class GuardarOrdenDiaRequestDto {
    private Long asambleaId;
    private Boolean esDefinitivo;
    private List<PuntoOrdenDiaItemInputDto> puntos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PuntoOrdenDiaItemInputDto {
        private Integer orden;
        private String titulo;
        private String descripcion;
    }
}
