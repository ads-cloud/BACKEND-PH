package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuorumResponseDto {
    private Long asambleaId;
    private String tituloAsamblea;
    private Long totalUnidadesAsistentes;
    private Long totalUnidadesCopropiedad;
    private Double coeficienteAcumulado;
    private Double porcentajeQuorum; // % relative to total 100%
    private Boolean alcanzoQuorum;   // > 50.0000%
    private LocalDateTime timestamp;
}
