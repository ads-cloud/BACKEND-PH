package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoOpcionDto {
    private Long opcionId;
    private String textoOpcion;
    private Long totalVotosUnidades;
    private Double sumaSistemaCoeficientes;
    private Double sumaCoeficiente;
    private Double porcentajePonderado; // Coeficiente de la opción / Coeficiente Total Votado * 100
}
