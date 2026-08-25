package com.ph.backend.dto;

import com.ph.backend.model.PreguntaStatus;
import com.ph.backend.model.PreguntaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoPreguntaDto {
    private Long preguntaId;
    private String enunciado;
    private PreguntaStatus estado;
    private PreguntaType tipoMayoria;
    private Double quorumPresenteAtVoto;
    private Double totalCoeficienteVotado;
    private Long totalUnidadesVotaron;
    private Long totalUnidadesAsistentes;
    private Boolean aprobado;
    private String mensajeResultado;
    private List<ResultadoOpcionDto> opciones;
}
