package com.ph.backend.controller;

import com.ph.backend.dto.HabitanteRequestDTO;
import com.ph.backend.model.UnidadHabitante;
import com.ph.backend.service.UnidadHabitanteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/unidades/{unidadId}/habitantes")
@RequiredArgsConstructor
@Slf4j
public class UnidadHabitanteController {

    private final UnidadHabitanteService habitanteService;

    @GetMapping
    public ResponseEntity<List<UnidadHabitante>> listarHabitantesPorUnidad(@PathVariable Long unidadId) {
        return ResponseEntity.ok(habitanteService.obtenerHabitantesPorUnidad(unidadId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> agregarOActualizarHabitante(@PathVariable Long unidadId, @RequestBody HabitanteRequestDTO dto) {
        try {
            UnidadHabitante guardado = habitanteService.agregarOActualizarHabitante(unidadId, dto);
            return ResponseEntity.ok(guardado);
        } catch (IllegalArgumentException e) {
            String rawMsg = e.getMessage() != null ? e.getMessage() : "";
            boolean requiereConfirmacionApod = rawMsg.startsWith("REQUIERE_CONFIRMACION_APODERADO:") || rawMsg.contains("ya cuenta con el Apoderado");
            boolean requiereConfirmacionProp = rawMsg.startsWith("REQUIERE_CONFIRMACION:") || rawMsg.contains("ya cuenta con el Propietario");
            if (requiereConfirmacionProp || requiereConfirmacionApod) {
                String cleanMsg = rawMsg
                        .replace("REQUIERE_CONFIRMACION_APODERADO:", "")
                        .replace("REQUIERE_CONFIRMACION:", "")
                        .trim();
                return ResponseEntity.ok(Map.of(
                        "status", "REQUIERE_CONFIRMACION",
                        "message", cleanMsg,
                        "requiereConfirmacionPropietario", requiereConfirmacionProp,
                        "requiereConfirmacionApoderado", requiereConfirmacionApod,
                        "requiereConfirmacion", true
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{habitanteId}/apoderado")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> cambiarDesignacionApoderado(@PathVariable Long unidadId, 
                                                         @PathVariable Long habitanteId, 
                                                         @RequestParam boolean esApoderado,
                                                         @RequestParam(required = false) Boolean confirmarCambio) {
        try {
            habitanteService.cambiarDesignacionApoderado(unidadId, habitanteId, esApoderado, confirmarCambio);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Estado de apoderado actualizado correctamente"));
        } catch (IllegalArgumentException e) {
            String rawMsg = e.getMessage() != null ? e.getMessage() : "";
            boolean requiereConfirmacionApod = rawMsg.startsWith("REQUIERE_CONFIRMACION_APODERADO:") || rawMsg.contains("ya cuenta con el Apoderado");
            boolean requiereConfirmacionProp = rawMsg.startsWith("REQUIERE_CONFIRMACION:") || rawMsg.contains("ya cuenta con el Propietario");
            if (requiereConfirmacionProp || requiereConfirmacionApod) {
                String cleanMsg = rawMsg
                        .replace("REQUIERE_CONFIRMACION_APODERADO:", "")
                        .replace("REQUIERE_CONFIRMACION:", "")
                        .trim();
                return ResponseEntity.ok(Map.of(
                        "status", "REQUIERE_CONFIRMACION",
                        "message", cleanMsg,
                        "requiereConfirmacionApoderado", requiereConfirmacionApod,
                        "requiereConfirmacionPropietario", requiereConfirmacionProp,
                        "requiereConfirmacion", true
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{habitanteId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> eliminarHabitante(@PathVariable Long unidadId, @PathVariable Long habitanteId) {
        try {
            habitanteService.eliminarHabitante(unidadId, habitanteId);
            return ResponseEntity.ok(Map.of("message", "Habitante removido exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
