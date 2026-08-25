package com.ph.backend.controller;

import com.ph.backend.model.Modulo;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.UsuarioRepository;
import com.ph.backend.service.ModuloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/modulos")
@RequiredArgsConstructor
@Slf4j
public class ModuloController {

    private final ModuloService moduloService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<?> obtenerModulosAutorizados(@RequestParam(required = false) Long copropiedadId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "No hay sesión activa"));
        }

        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuario no encontrado"));
        }

        List<Modulo> modulos = moduloService.obtenerModulosAutorizados(usuario, copropiedadId);
        return ResponseEntity.ok(modulos);
    }

    // --- Módulo Exclusivo SuperAdmin: Administración Global del Catálogo de Módulos ---
    @GetMapping("/catalogo-global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> obtenerCatalogoGlobalModulos() {
        return ResponseEntity.ok(moduloService.obtenerCatalogoGlobalModulos());
    }

    // --- Permisos Nivel 1: SuperAdmin configura Módulos habilitados por Copropiedad ---
    @GetMapping("/copropiedad/{copropiedadId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> obtenerModulosCopropiedad(@PathVariable Long copropiedadId) {
        return ResponseEntity.ok(moduloService.obtenerModulosCopropiedad(copropiedadId));
    }

    @PutMapping("/copropiedad/{copropiedadId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> guardarModulosCopropiedad(@PathVariable Long copropiedadId, @RequestBody List<Long> modulosActivosIds) {
        moduloService.guardarModulosCopropiedad(copropiedadId, modulosActivosIds);
        return ResponseEntity.ok(Map.of("message", "Módulos de la copropiedad actualizados correctamente"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> obtenerTodosLosModulos(@RequestParam(required = false) Long copropiedadId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "No hay sesión activa"));
        }

        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuario no encontrado"));
        }

        List<Modulo> modulos = moduloService.obtenerModulosParaAdministracion(usuario, copropiedadId);
        return ResponseEntity.ok(modulos);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> crearOActualizarModulo(@RequestBody Modulo modulo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin && ("MONITOREO".equals(modulo.getCodigo()) || "ADMIN_PH".equals(modulo.getCodigo()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Acceso denegado: este módulo es exclusivo del SuperAdministrador."));
        }

        Modulo guardado = moduloService.guardarOActualizarModulo(modulo);
        return ResponseEntity.ok(guardado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarModulo(@PathVariable Long id, @RequestBody Modulo modulo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin && ("MONITOREO".equals(modulo.getCodigo()) || "ADMIN_PH".equals(modulo.getCodigo()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Acceso denegado: este módulo es exclusivo del SuperAdministrador."));
        }

        modulo.setId(id);
        Modulo guardado = moduloService.guardarOActualizarModulo(modulo);
        return ResponseEntity.ok(guardado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstadoModulo(@PathVariable Long id, @RequestParam boolean activo) {
        moduloService.cambiarEstadoModulo(id, activo);
        return ResponseEntity.ok(Map.of("message", "Estado de módulo actualizado correctamente"));
    }
}
