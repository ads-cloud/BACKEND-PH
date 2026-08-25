package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambiarPasswordInicialDto {
    private String username;
    private String passwordActual;
    private String nuevaPassword;
}
