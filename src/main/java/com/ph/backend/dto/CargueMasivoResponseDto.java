package com.ph.backend.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CargueMasivoResponseDto {
    private int totalProcesados;
    private int exitosos;
    private int fallidos;
    private List<String> errores;
    private String mensaje;
}
