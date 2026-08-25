package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoOrdenDiaDto {
    private Long id;
    private Integer orden;
    private String titulo;
    private String descripcion;
    private Boolean completado;
    private LocalDateTime fechaCompletado;
}
