package com.ph.backend.dto;

import com.ph.backend.model.AsambleaType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAsambleaDto {
    private String titulo;
    private LocalDateTime fecha;
    private AsambleaType tipo;
    private Long copropiedadId;
    private java.util.List<GuardarOrdenDiaRequestDto.PuntoOrdenDiaItemInputDto> ordenDia;
}
