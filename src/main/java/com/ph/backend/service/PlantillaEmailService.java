package com.ph.backend.service;

import com.ph.backend.dto.RenderedEmailDto;
import com.ph.backend.model.PlantillaEmail;
import com.ph.backend.repository.PlantillaEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlantillaEmailService {

    private final PlantillaEmailRepository plantillaEmailRepository;

    @Transactional(readOnly = true)
    public RenderedEmailDto procesarPlantilla(String codigo, Long copropiedadId, Map<String, String> variables) {
        PlantillaEmail plantilla = obtenerMejorPlantilla(codigo, copropiedadId);

        String asuntoProcesado = reemplazarVariables(plantilla.getAsunto(), variables);
        String htmlProcesado = reemplazarVariables(plantilla.getCuerpoHtml(), variables);

        return RenderedEmailDto.builder()
                .asunto(asuntoProcesado)
                .cuerpoHtml(htmlProcesado)
                .build();
    }

    @Transactional(readOnly = true)
    public PlantillaEmail obtenerMejorPlantilla(String codigo, Long copropiedadId) {
        if (copropiedadId != null) {
            Optional<PlantillaEmail> tenantSpec = plantillaEmailRepository.findByCodigoAndCopropiedadId(codigo, copropiedadId);
            if (tenantSpec.isPresent() && Boolean.TRUE.equals(tenantSpec.get().getActivo())) {
                return tenantSpec.get();
            }
        }

        Optional<PlantillaEmail> globalSpec = plantillaEmailRepository.findByCodigoAndCopropiedadIdIsNull(codigo);
        if (globalSpec.isPresent()) {
            return globalSpec.get();
        }

        // Fallback dinámico si no existe la plantilla
        return PlantillaEmail.builder()
                .codigo(codigo)
                .nombre("Plantilla Genérica")
                .asunto("Notificación de proyectoPH")
                .cuerpoHtml("<div style='font-family: Arial, sans-serif; padding: 20px;'><h2>Notificación proyectoPH</h2><p>{{contenido}}</p></div>")
                .build();
    }

    private String reemplazarVariables(String texto, Map<String, String> variables) {
        if (texto == null || texto.isBlank()) return "";
        if (variables == null || variables.isEmpty()) return texto;

        String resultado = texto;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String valor = entry.getValue() != null ? entry.getValue() : "";
            resultado = resultado.replace(placeholder, valor);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<PlantillaEmail> listarPlantillas(Long copropiedadId) {
        if (copropiedadId != null) {
            List<PlantillaEmail> tenantPlantillas = plantillaEmailRepository.findByCopropiedadId(copropiedadId);
            List<PlantillaEmail> globales = plantillaEmailRepository.findByCopropiedadIdIsNull();

            // Combinar asegurando que las del tenant sobrescriban a las globales para el mismo código
            for (PlantillaEmail global : globales) {
                boolean existeEnTenant = tenantPlantillas.stream()
                        .anyMatch(t -> t.getCodigo().equalsIgnoreCase(global.getCodigo()));
                if (!existeEnTenant) {
                    tenantPlantillas.add(global);
                }
            }
            return tenantPlantillas;
        }
        return plantillaEmailRepository.findByCopropiedadIdIsNull();
    }

    @Transactional
    public PlantillaEmail guardarOActualizarPlantilla(Long copropiedadId, String codigo, String nombre, String asunto, String cuerpoHtml, String variablesDisponibles) {
        Optional<PlantillaEmail> existente = copropiedadId != null ?
                plantillaEmailRepository.findByCodigoAndCopropiedadId(codigo, copropiedadId) :
                plantillaEmailRepository.findByCodigoAndCopropiedadIdIsNull(codigo);

        PlantillaEmail plantilla;
        if (existente.isPresent()) {
            plantilla = existente.get();
            plantilla.setNombre(nombre);
            plantilla.setAsunto(asunto);
            plantilla.setCuerpoHtml(cuerpoHtml);
            if (variablesDisponibles != null) {
                plantilla.setVariablesDisponibles(variablesDisponibles);
            }
        } else {
            plantilla = PlantillaEmail.builder()
                    .copropiedadId(copropiedadId)
                    .codigo(codigo)
                    .nombre(nombre)
                    .asunto(asunto)
                    .cuerpoHtml(cuerpoHtml)
                    .variablesDisponibles(variablesDisponibles)
                    .activo(true)
                    .build();
        }

        return plantillaEmailRepository.save(plantilla);
    }

    @Transactional
    public void restaurarPlantillaDefault(Long copropiedadId, String codigo) {
        if (copropiedadId != null) {
            plantillaEmailRepository.findByCodigoAndCopropiedadId(codigo, copropiedadId)
                    .ifPresent(plantillaEmailRepository::delete);
        }
    }
}
