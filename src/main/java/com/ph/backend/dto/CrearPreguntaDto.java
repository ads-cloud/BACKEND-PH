package com.ph.backend.dto;

import com.ph.backend.model.PreguntaType;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPreguntaDto {
    private Long asambleaId;
    private String enunciado;
    private PreguntaType tipoMayoria;
    private Integer duracionMinutos;
    private List<String> opciones;
}
