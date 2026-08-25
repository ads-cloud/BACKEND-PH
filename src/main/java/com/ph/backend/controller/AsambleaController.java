package com.ph.backend.controller;

import com.ph.backend.dto.AsistenciaRequestDto;
import com.ph.backend.dto.QuorumResponseDto;
import com.ph.backend.model.Asamblea;
import com.ph.backend.model.AsistenciaAsamblea;
import com.ph.backend.repository.AsambleaRepository;
import com.ph.backend.service.AsambleaService;
import com.ph.backend.service.QuorumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asambleas")
@RequiredArgsConstructor
public class AsambleaController {

    private final AsambleaRepository asambleaRepository;
    private final AsambleaService asambleaService;
    private final QuorumService quorumService;

    @GetMapping
    public ResponseEntity<List<Asamblea>> listarAsambleas() {
        Long tenantId = com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        return ResponseEntity.ok(asambleaRepository.findByCopropiedadId(tenantId));
    }

    @PostMapping
    public ResponseEntity<Asamblea> crearAsamblea(@RequestBody com.ph.backend.dto.CrearAsambleaDto dto) {
        return ResponseEntity.ok(asambleaService.crearAsamblea(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asamblea> obtenerAsamblea(@PathVariable Long id) {
        return asambleaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Asamblea> cambiarEstadoAsamblea(@PathVariable Long id, @RequestParam com.ph.backend.model.AsambleaStatus estado) {
        return ResponseEntity.ok(asambleaService.cambiarEstadoAsamblea(id, estado));
    }

    @GetMapping("/{id}/quorum")
    public ResponseEntity<QuorumResponseDto> obtenerQuorum(@PathVariable Long id) {
        return ResponseEntity.ok(quorumService.calcularQuorum(id));
    }

    @PostMapping("/asistencia")
    public ResponseEntity<AsistenciaAsamblea> tomarAsistencia(@RequestBody AsistenciaRequestDto dto) {
        return ResponseEntity.ok(asambleaService.registrarAsistencia(dto));
    }

    @PostMapping("/validar-pin")
    public ResponseEntity<AsistenciaAsamblea> validarPinAsamblea(@RequestBody com.ph.backend.dto.ConfirmarPinAsambleaDto dto) {
        return ResponseEntity.ok(asambleaService.confirmarPinAsamblea(dto));
    }

    @GetMapping("/{id}/asistentes")
    public ResponseEntity<List<AsistenciaAsamblea>> listarAsistentes(@PathVariable Long id) {
        return ResponseEntity.ok(asambleaService.obtenerAsistentes(id));
    }

    @PatchMapping("/{id}/registro-cerrado")
    public ResponseEntity<Asamblea> cambiarEstadoRegistro(
            @PathVariable Long id,
            @RequestParam boolean cerrado) {
        return ResponseEntity.ok(asambleaService.cambiarEstadoRegistro(id, cerrado));
    }

    @GetMapping("/{id}/orden-dia")
    public ResponseEntity<com.ph.backend.dto.OrdenDiaResponseDto> obtenerOrdenDia(@PathVariable Long id) {
        return ResponseEntity.ok(asambleaService.obtenerOrdenDia(id));
    }

    @PostMapping("/{id}/orden-dia")
    public ResponseEntity<com.ph.backend.dto.OrdenDiaResponseDto> guardarOrdenDia(
            @PathVariable Long id,
            @RequestBody com.ph.backend.dto.GuardarOrdenDiaRequestDto dto) {
        dto.setAsambleaId(id);
        return ResponseEntity.ok(asambleaService.guardarOrdenDia(dto));
    }

    @PatchMapping("/orden-dia/puntos/{puntoId}/completado")
    public ResponseEntity<com.ph.backend.dto.PuntoOrdenDiaDto> marcarPuntoCompletado(
            @PathVariable Long puntoId,
            @RequestParam boolean completado) {
        return ResponseEntity.ok(asambleaService.marcarPuntoOrdenDiaCompletado(puntoId, completado));
    }
}
