package com.ph.backend.controller;

import com.ph.backend.dto.ResultadoPreguntaDto;
import com.ph.backend.model.Pregunta;
import com.ph.backend.model.PreguntaStatus;
import com.ph.backend.repository.PreguntaRepository;
import com.ph.backend.service.AsambleaService;
import com.ph.backend.service.VotacionService;
import com.ph.backend.service.AsambleaWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preguntas")
@RequiredArgsConstructor
public class PreguntaController {

    private final PreguntaRepository preguntaRepository;
    private final AsambleaService asambleaService;
    private final VotacionService votacionService;
    private final AsambleaWebSocketPublisher webSocketPublisher;

    @GetMapping("/asamblea/{asambleaId}")
    public ResponseEntity<List<Pregunta>> listarPreguntasPorAsamblea(@PathVariable Long asambleaId) {
        return ResponseEntity.ok(asambleaService.obtenerPreguntasPorAsamblea(asambleaId));
    }

    @PostMapping
    public ResponseEntity<Pregunta> crearPregunta(@RequestBody com.ph.backend.dto.CrearPreguntaDto dto) {
        Pregunta p = asambleaService.crearPregunta(dto);
        if (p != null && p.getAsamblea() != null) {
            webSocketPublisher.notifyPreguntasUpdated(p.getAsamblea().getId());
        }
        return ResponseEntity.ok(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pregunta> obtenerPregunta(@PathVariable Long id) {
        return preguntaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pregunta> cambiarEstado(
            @PathVariable Long id,
            @RequestParam PreguntaStatus estado,
            @RequestParam(required = false) Integer duracionMinutos) {
        Pregunta p = asambleaService.cambiarEstadoPregunta(id, estado, duracionMinutos);
        if (p != null && p.getAsamblea() != null) {
            webSocketPublisher.notifyPreguntasUpdated(p.getAsamblea().getId());
        }
        return ResponseEntity.ok(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pregunta> editarPregunta(
            @PathVariable Long id,
            @RequestBody com.ph.backend.dto.CrearPreguntaDto dto) {
        return ResponseEntity.ok(asambleaService.editarPregunta(id, dto));
    }

    @GetMapping("/{id}/resultados")
    public ResponseEntity<ResultadoPreguntaDto> obtenerResultados(@PathVariable Long id) {
        return ResponseEntity.ok(votacionService.obtenerResultados(id));
    }
}
