package com.ph.backend.controller;

import com.ph.backend.dto.AuthResponseDto;
import com.ph.backend.dto.IngresoCopropietarioDto;
import com.ph.backend.dto.VotoRequestDto;
import com.ph.backend.model.VotoEmitido;
import com.ph.backend.service.AsambleaService;
import com.ph.backend.service.VotacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/votaciones")
@RequiredArgsConstructor
public class VotacionController {

    private final VotacionService votacionService;
    private final AsambleaService asambleaService;

    @PostMapping("/login-copropietario")
    public ResponseEntity<AuthResponseDto> loginCopropietario(@RequestBody IngresoCopropietarioDto dto, jakarta.servlet.http.HttpServletResponse response) {
        AuthResponseDto resDto = asambleaService.autenticarCopropietario(dto);
        if (resDto != null && resDto.getToken() != null) {
            org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("jwt_token", resDto.getToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(-1)
                    .build();
            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return ResponseEntity.ok(resDto);
    }

    @PostMapping("/emitir")
    public ResponseEntity<VotoEmitido> emitirVoto(@RequestBody VotoRequestDto dto) {
        return ResponseEntity.ok(votacionService.registrarVoto(dto));
    }

    @GetMapping("/unidad/{unidadId}")
    public ResponseEntity<java.util.Map<Long, Long>> obtenerVotosUnidad(@PathVariable Long unidadId) {
        return ResponseEntity.ok(votacionService.obtenerVotosPorUnidad(unidadId));
    }
}
