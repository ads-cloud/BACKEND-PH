package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificarPinDto {
    private String documento;
    private String email;
    private String pin;
}
