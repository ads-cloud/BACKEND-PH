package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitarResetPasswordDto {
    private String documento;
    private String email;
}
