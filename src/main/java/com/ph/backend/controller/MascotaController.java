package com.ph.backend.controller;

import com.ph.backend.model.Mascota;
import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.repository.MascotaRepository;
import com.ph.backend.repository.UnidadPrivadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/unidades/{unidadId}/mascotas")
@RequiredArgsConstructor
@Slf4j
public class MascotaController {

    private final MascotaRepository mascotaRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;

    @GetMapping
    public ResponseEntity<List<Mascota>> listarMascotas(@PathVariable Long unidadId) {
        return ResponseEntity.ok(mascotaRepository.findByUnidadId(unidadId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> agregarMascota(@PathVariable Long unidadId, @RequestBody Mascota mascotaReq) {
        UnidadPrivada unidad = unidadPrivadaRepository.findById(unidadId)
                .orElseThrow(() -> new IllegalArgumentException("Unidad inmobiliaria no encontrada: " + unidadId));

        if (mascotaReq.getNombre() == null || mascotaReq.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre de la mascota es obligatorio"));
        }

        mascotaReq.setId(null);
        mascotaReq.setUnidad(unidad);
        mascotaReq.setNombre(mascotaReq.getNombre().trim());
        if (mascotaReq.getTipo() == null || mascotaReq.getTipo().trim().isEmpty()) {
            mascotaReq.setTipo("PERRO");
        }
        if (mascotaReq.getVacunasAlDia() == null) {
            mascotaReq.setVacunasAlDia(true);
        }

        Mascota guardada = mascotaRepository.save(mascotaReq);
        return ResponseEntity.ok(guardada);
    }

    @DeleteMapping("/{mascotaId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> eliminarMascota(@PathVariable Long unidadId, @PathVariable Long mascotaId) {
        if (!mascotaRepository.existsById(mascotaId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mascota no encontrada"));
        }
        mascotaRepository.deleteById(mascotaId);
        return ResponseEntity.ok(Map.of("message", "Mascota eliminada exitosamente"));
    }
}
