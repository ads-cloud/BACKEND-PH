package com.ph.backend.service;

import com.ph.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsernameService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Genera un nombre de usuario único con formato nombre.apellido.
     * Si el nombre de usuario ya existe en la base de datos, le añade un número consecutivo (ej. carlos.perez1, carlos.perez2).
     */
    public String generarUsernameUnico(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            nombreCompleto = "usuario";
        }

        // Limpiar acentos y tildes (NFD) y caracteres no alfanuméricos
        String normalizado = Normalizer.normalize(nombreCompleto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", "");

        String[] partes = normalizado.split("\\s+");
        String baseUsername;

        if (partes.length == 1) {
            baseUsername = partes[0];
        } else if (partes.length == 2 || partes.length == 3) {
            // "Carlos Pérez" -> carlos.perez
            // "Carlos Pérez Gómez" -> carlos.perez
            baseUsername = partes[0] + "." + partes[1];
        } else {
            // 4 o más nombres: "Carlos Alberto Pérez Gómez" -> carlos.perez (partes[0] + partes[2])
            baseUsername = partes[0] + "." + partes[2];
        }

        if (baseUsername.isBlank()) {
            baseUsername = "usuario";
        }

        String candidato = baseUsername;
        int contador = 1;

        while (usuarioRepository.findByUsername(candidato).isPresent()) {
            candidato = baseUsername + contador;
            contador++;
        }

        log.info("[USERNAME SERVICE] 👤 Nombre de usuario único generado: '{}' para nombre completo: '{}'", candidato, nombreCompleto);
        return candidato;
    }
}
