package com.ph.backend.controller;

import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.model.Vehiculo;
import com.ph.backend.repository.UnidadPrivadaRepository;
import com.ph.backend.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/unidades/{unidadId}/vehiculos")
@RequiredArgsConstructor
@Slf4j
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;

    @GetMapping
    public ResponseEntity<List<Vehiculo>> listarVehiculos(@PathVariable Long unidadId) {
        return ResponseEntity.ok(vehiculoRepository.findByUnidadId(unidadId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> agregarVehiculo(@PathVariable Long unidadId, @RequestBody Vehiculo vehiculoReq) {
        UnidadPrivada unidad = unidadPrivadaRepository.findById(unidadId)
                .orElseThrow(() -> new IllegalArgumentException("Unidad inmobiliaria no encontrada: " + unidadId));

        if (vehiculoReq.getPlaca() == null || vehiculoReq.getPlaca().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La placa es obligatoria"));
        }

        vehiculoReq.setId(null);
        vehiculoReq.setUnidad(unidad);
        vehiculoReq.setPlaca(vehiculoReq.getPlaca().trim().toUpperCase());
        if (vehiculoReq.getTipo() == null || vehiculoReq.getTipo().trim().isEmpty()) {
            vehiculoReq.setTipo("CARRO");
        }

        Vehiculo guardado = vehiculoRepository.save(vehiculoReq);
        return ResponseEntity.ok(guardado);
    }

    @DeleteMapping("/{vehiculoId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long unidadId, @PathVariable Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vehículo no encontrado"));
        }
        vehiculoRepository.deleteById(vehiculoId);
        return ResponseEntity.ok(Map.of("message", "Vehículo eliminado exitosamente"));
    }
}
