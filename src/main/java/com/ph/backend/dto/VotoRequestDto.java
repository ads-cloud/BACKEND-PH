package com.ph.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotoRequestDto {
    private Long preguntaId;

    @JsonAlias({"opcionId", "opcionVotoId"})
    private Long opcionVotoId;

    private Long unidadPrivadaId;
    private String cedulaPersona;

    public void setOpcionId(Long opcionId) {
        this.opcionVotoId = opcionId;
    }
}
