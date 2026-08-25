package com.ph.backend.dto;

import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponseDto {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long usuarioId;
    private Long copropiedadId;
    private String copropiedadNombre;
    private String documento;
    private String username;
    private String nombreCompleto;
    private Set<Rol> roles;
    private Boolean isSuperAdmin;
    private Boolean debeCambiarPassword;
    private List<Copropiedad> copropiedadesAsignadas;
    private Long asambleaActivaId;
    private List<UnidadRepresentadaDto> unidadesRepresentadas;
    private String email;
    private String telefono;
}
