package com.ph.backend.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class PasswordGeneratorService {

    private static final String LETRAS_MAYUS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluye O, I para evitar confusión
    private static final String LETRAS_MINUS = "abcdefghijkmnpqrstuvwxyz"; // Excluye l, o
    private static final String NUMEROS = "23456789";                   // Excluye 0, 1
    private static final String ESPECIALES = "!@#$%*?";

    private final SecureRandom random = new SecureRandom();

    public String generarPasswordSegura() {
        StringBuilder sb = new StringBuilder();

        // 1. Garantizar al menos 1 Mayúscula, 1 Minúscula y 1 Número
        sb.append(LETRAS_MAYUS.charAt(random.nextInt(LETRAS_MAYUS.length())));
        sb.append(LETRAS_MINUS.charAt(random.nextInt(LETRAS_MINUS.length())));
        sb.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));

        // 2. Rellenar letras/números aleatorios hasta completar 7 caracteres
        String combinados = LETRAS_MAYUS + LETRAS_MINUS + NUMEROS;
        for (int i = 0; i < 4; i++) {
            sb.append(combinados.charAt(random.nextInt(combinados.length())));
        }

        // 3. Mezclar los primeros 7 caracteres
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        // 4. Agregar obligatoriamente el carácter especial al final
        char caracterEspecial = ESPECIALES.charAt(random.nextInt(ESPECIALES.length()));

        return new String(chars) + caracterEspecial;
    }
}
