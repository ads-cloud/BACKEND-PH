package com.ph.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    private String username;
    private String documento;
    private String password;
    private Boolean rememberMe;

    public LoginRequestDto(String usernameOrDocumento, String password) {
        this.username = usernameOrDocumento;
        this.documento = usernameOrDocumento;
        this.password = password;
    }

    public String getUsername() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return documento;
    }
}
