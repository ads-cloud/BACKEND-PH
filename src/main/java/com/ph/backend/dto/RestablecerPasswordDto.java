package com.ph.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestablecerPasswordDto {
    private String documento;
    private String email;
    private String pin;
    private String nuevaPassword;
}
