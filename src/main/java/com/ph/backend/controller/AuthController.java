package com.ph.backend.controller;

import com.ph.backend.dto.*;
import com.ph.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponseDto> authenticateUser(@RequestBody LoginRequestDto loginDto, HttpServletResponse response) {
        JwtAuthResponseDto dto = authService.login(loginDto);
        boolean rememberMe = Boolean.TRUE.equals(loginDto.getRememberMe());
        setJwtCookie(response, dto.getAccessToken(), rememberMe);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/solicitar-clave")
    public ResponseEntity<Map<String, String>> solicitarClave(@RequestBody SolicitarClaveRequestDto dto) {
        String mensaje = authService.solicitarClaveOtp(dto);
        return ResponseEntity.ok(Map.of("message", mensaje));
    }

    @PostMapping("/login-otp")
    public ResponseEntity<JwtAuthResponseDto> loginConOtp(@RequestBody LoginOtpRequestDto dto, HttpServletResponse response) {
        JwtAuthResponseDto resDto = authService.loginConOtp(dto);
        setJwtCookie(response, resDto.getAccessToken(), false);
        return ResponseEntity.ok(resDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "No hay sesión de usuario activa"));
        }
        try {
            return ResponseEntity.ok(authService.obtenerPerfilActual(auth));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password/solicitar-pin")
    public ResponseEntity<Map<String, String>> solicitarPinResetPassword(@RequestBody SolicitarResetPasswordDto dto) {
        String mensaje = authService.solicitarPinResetPassword(dto);
        return ResponseEntity.ok(Map.of("message", mensaje));
    }

    @PostMapping("/reset-password/verificar-pin")
    public ResponseEntity<Map<String, String>> verificarPinResetPassword(@RequestBody VerificarPinDto dto) {
        String mensaje = authService.verificarPinResetPassword(dto);
        return ResponseEntity.ok(Map.of("message", mensaje));
    }

    @PostMapping("/reset-password/confirmar")
    public ResponseEntity<Map<String, String>> confirmarResetPassword(@RequestBody RestablecerPasswordDto dto) {
        String mensaje = authService.confirmarResetPassword(dto);
        return ResponseEntity.ok(Map.of("message", mensaje));
    }

    @PostMapping("/cambiar-password-inicial")
    public ResponseEntity<JwtAuthResponseDto> cambiarPasswordInicial(@RequestBody CambiarPasswordInicialDto dto, HttpServletResponse response) {
        JwtAuthResponseDto resDto = authService.cambiarPasswordInicial(dto);
        setJwtCookie(response, resDto.getAccessToken(), false);
        return ResponseEntity.ok(resDto);
    }

    @PutMapping("/perfil")
    public ResponseEntity<JwtAuthResponseDto> actualizarPerfil(@RequestBody ActualizarPerfilDto dto) {
        return ResponseEntity.ok(authService.actualizarPerfil(dto));
    }

    @GetMapping("/perfil/{documento}")
    public ResponseEntity<JwtAuthResponseDto> obtenerPerfil(@PathVariable String documento) {
        return ResponseEntity.ok(authService.obtenerPerfil(documento));
    }

    private void setJwtCookie(HttpServletResponse response, String token, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax");

        if (rememberMe) {
            cookieBuilder.maxAge(7 * 24 * 60 * 60); // 7 días
        } else {
            cookieBuilder.maxAge(-1); // Cookie de sesión (caduca al cerrar navegador)
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
