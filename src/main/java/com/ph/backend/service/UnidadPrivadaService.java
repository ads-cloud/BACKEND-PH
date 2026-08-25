package com.ph.backend.service;

import com.ph.backend.dto.ActualizarUnidadDto;
import com.ph.backend.dto.CargueMasivoResponseDto;
import com.ph.backend.dto.CrearUnidadDto;
import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Persona;
import com.ph.backend.model.Rol;
import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.CopropiedadRepository;
import com.ph.backend.repository.PersonaRepository;
import com.ph.backend.repository.UnidadPrivadaRepository;
import com.ph.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnidadPrivadaService {

    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final CopropiedadRepository copropiedadRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameService usernameService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final NotificacionService notificacionService;

    public List<UnidadPrivada> obtenerMisInmuebles(String documento) {
        if (documento == null || documento.isBlank()) {
            return List.of();
        }
        return unidadPrivadaRepository.findMisInmueblesPorDocumento(documento.trim());
    }

    @Transactional
    public UnidadPrivada crearUnidad(CrearUnidadDto dto) {
        Long copId = dto.getCopropiedadId() != null ? dto.getCopropiedadId()
                : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        Copropiedad copropiedad = copropiedadRepository.findById(copId)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada con ID: " + copId));

        // Buscar o autocrear Persona como Propietario con datos completos del DTO solo si se especifica cédula
        String cedulaProp = (dto.getCedulaPropietario() != null && !dto.getCedulaPropietario().isBlank()) 
                ? dto.getCedulaPropietario().trim() 
                : null;
        
        Persona propietario = null;
        if (cedulaProp != null) {
            propietario = obtenerOCrearPropietario(
                    copId,
                    cedulaProp,
                    dto.getTorre(),
                    dto.getNumeroUnidad(),
                    dto.getNombrePropietario(),
                    dto.getEmailPropietario(),
                    dto.getTelefonoPropietario());
        }

        // Buscar o crear Apoderado si fue especificado (se inicializa con poderAprobado
        // = false)
        Usuario apoderado = null;
        boolean poderAprobado = false;
        if (dto.getDocumentoApoderado() != null && !dto.getDocumentoApoderado().isBlank()) {
            String docApoderado = dto.getDocumentoApoderado().trim();
            apoderado = obtenerOCrearUsuarioApoderado(copId, docApoderado, dto.getNombreApoderado(),
                    dto.getEmailApoderado(), dto.getTelefonoApoderado());
        }

        UnidadPrivada unidad = UnidadPrivada.builder()
                .copropiedad(copropiedad)
                .torre(dto.getTorre().trim())
                .numeroUnidad(dto.getNumeroUnidad().trim())
                .coeficiente(dto.getCoeficiente() != null ? dto.getCoeficiente() : 5.0)
                .propietario(propietario)
                .apoderado(apoderado)
                .poderAprobado(poderAprobado)
                .build();

        UnidadPrivada guardada = unidadPrivadaRepository.save(unidad);
        return guardada;
    }

    public byte[] generarPlantillaExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Plantilla Unidades");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "torre", "numeroUnidad", "coeficiente", "cedulaPropietario",
                    "nombrePropietario", "emailPropietario", "telefonoPropietario",
                    "documentoApoderado", "nombreApoderado", "emailApoderado", "telefonoApoderado"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Object[][] sampleData = {
                    { "1", "101", 20.0, "10101010", "Carlos Pérez", "carlos.perez@gmail.com", "3001234567", "", "", "",
                            "" },
                    { "1", "102", 25.0, "20202020", "María Rodríguez", "maria.rodriguez@gmail.com", "3002345678", "",
                            "", "", "" },
                    { "2", "201", 15.0, "30303030", "Juan Gómez", "juan.gomez@gmail.com", "3003456789", "", "", "",
                            "" },
                    { "2", "202", 15.0, "40404040", "Ana Martínez", "ana.martinez@gmail.com", "3004567890", "", "", "",
                            "" },
                    { "3", "301", 10.0, "50505050", "Roberto Silva", "roberto.silva@gmail.com", "3005678901",
                            "77777777", "Carlos Mario Gómez", "apoderado.carlos@gmail.com", "3109876543" },
                    { "3", "302", 15.0, "60606060", "Laura Morales", "laura.morales@gmail.com", "3006789012", "", "",
                            "", "" }
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = sampleData[r][c];
                    if (val instanceof Double) {
                        cell.setCellValue((Double) val);
                    } else {
                        cell.setCellValue(String.valueOf(val));
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            // Escribir también a la carpeta pública del frontend
            try {
                java.io.File filePub = new java.io.File(
                        "c:/Desarrollo/proyectoPh/frontend/public/plantillas/plantilla_cargue_unidades.xlsx");
                filePub.getParentFile().mkdirs();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePub)) {
                    fos.write(bytes);
                }
            } catch (Exception exFile) {
                log.warn("[BACKEND UNIDAD] No se pudo escribir plantilla en public frontend: {}", exFile.getMessage());
            }

            return bytes;
        } catch (Exception e) {
            log.error("[BACKEND UNIDAD] Error al generar la plantilla de Excel", e);
            throw new RuntimeException("Error al generar la plantilla de Excel: " + e.getMessage());
        }
    }

    public CargueMasivoResponseDto procesarCargueMasivo(MultipartFile file, Long copropiedadId) {
        Long copId = copropiedadId != null ? copropiedadId : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        Copropiedad copropiedad = copropiedadRepository.findById(copId)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada con ID: " + copId));

        List<String> errores = new ArrayList<>();
        int exitosos = 0;
        int total = 0;

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        try {
            if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
                // Reader CSV
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        if (line.trim().isEmpty())
                            continue;
                        if (lineNum == 1
                                && (line.toLowerCase().contains("torre") || line.toLowerCase().contains("numero"))) {
                            continue; // Skip Header
                        }
                        total++;
                        String delimiter = line.contains(";") ? ";" : ",";
                        String[] parts = line.split(delimiter, -1);
                        if (parts.length < 4) {
                            errores.add("Línea " + lineNum
                                    + ": Faltan columnas requeridas (torre, apto, coeficiente, cedula).");
                            continue;
                        }
                        try {
                            String torre = parts[0].trim();
                            String apto = parts[1].trim();
                            double coef = parseDoubleSafe(parts[2].trim());
                            String cedProp = parts[3].trim();
                            String nombreProp = parts.length > 4 ? parts[4].trim() : "";
                            String emailProp = parts.length > 5 ? parts[5].trim() : "";
                            String telProp = parts.length > 6 ? parts[6].trim() : "";
                            String docApod = parts.length > 7 ? parts[7].trim() : "";
                            String nombreApod = parts.length > 8 ? parts[8].trim() : "";
                            String emailApod = parts.length > 9 ? parts[9].trim() : "";
                            String telApod = parts.length > 10 ? parts[10].trim() : "";

                            guardarUnidadSafe(copropiedad, torre, apto, coef, cedProp, nombreProp, emailProp, telProp,
                                    docApod, nombreApod, emailApod, telApod);
                            exitosos++;
                        } catch (Exception ex) {
                            errores.add("Línea " + lineNum + ": " + ex.getMessage());
                        }
                    }
                }
            } else {
                // Reader Excel (.xlsx / .xls) Apache POI
                try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null)
                            continue;

                        String cell0 = getCellStringValue(row.getCell(0));
                        if (i == 0
                                && (cell0.toLowerCase().contains("torre") || cell0.toLowerCase().contains("numero"))) {
                            continue; // Skip Header
                        }
                        if (cell0.isBlank() && getCellStringValue(row.getCell(1)).isBlank()) {
                            continue; // Skip Empty Row
                        }

                        total++;
                        try {
                            String torre = getCellStringValue(row.getCell(0));
                            String apto = getCellStringValue(row.getCell(1));
                            double coef = parseDoubleSafe(getCellStringValue(row.getCell(2)));
                            String cedProp = getCellStringValue(row.getCell(3));
                            String nombreProp = getCellStringValue(row.getCell(4));
                            String emailProp = getCellStringValue(row.getCell(5));
                            String telProp = getCellStringValue(row.getCell(6));
                            String docApod = getCellStringValue(row.getCell(7));
                            String nombreApod = getCellStringValue(row.getCell(8));
                            String emailApod = getCellStringValue(row.getCell(9));
                            String telApod = getCellStringValue(row.getCell(10));

                            if (torre.isBlank() || apto.isBlank() || cedProp.isBlank()) {
                                errores.add("Fila " + (i + 1) + ": Datos requeridos vacíos (Torre, Apto o Cédula).");
                                continue;
                            }

                            guardarUnidadSafe(copropiedad, torre, apto, coef, cedProp, nombreProp, emailProp, telProp,
                                    docApod, nombreApod, emailApod, telApod);
                            exitosos++;
                        } catch (Exception ex) {
                            errores.add("Fila " + (i + 1) + ": " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[BACKEND UNIDAD] Error procesando archivo masivo", e);
            throw new IllegalArgumentException("Error al leer el archivo de Excel/CSV: " + e.getMessage());
        }

        return CargueMasivoResponseDto.builder()
                .totalProcesados(total)
                .exitosos(exitosos)
                .fallidos(errores.size())
                .errores(errores)
                .mensaje(String.format("Procesados: %d | Exitosos: %d | Errores: %d", total, exitosos, errores.size()))
                .build();
    }

    private void guardarUnidadSafe(Copropiedad copropiedad, String torre, String apto, double coef, String cedProp,
            String nombreProp, String emailProp, String telProp, String docApod, String nombreApod, String emailApod,
            String telApod) {
        Persona propietario = obtenerOCrearPropietario(copropiedad.getId(), cedProp, torre, apto, nombreProp, emailProp,
                telProp);
        Usuario apoderado = null;
        if (!docApod.isBlank()) {
            apoderado = obtenerOCrearUsuarioApoderado(copropiedad.getId(), docApod, nombreApod, emailApod, telApod);
        }

        UnidadPrivada unidadExistente = unidadPrivadaRepository
                .findByCopropiedadIdAndTorreAndNumeroUnidad(copropiedad.getId(), torre, apto)
                .orElse(null);

        if (unidadExistente != null) {
            unidadExistente.setCoeficiente(coef);
            unidadExistente.setPropietario(propietario);
            if (apoderado != null) {
                unidadExistente.setApoderado(apoderado);
                unidadExistente.setPoderAprobado(false);
            }
            unidadPrivadaRepository.save(unidadExistente);
        } else {
            UnidadPrivada unidad = UnidadPrivada.builder()
                    .copropiedad(copropiedad)
                    .torre(torre)
                    .numeroUnidad(apto)
                    .coeficiente(coef)
                    .propietario(propietario)
                    .apoderado(apoderado)
                    .poderAprobado(false)
                    .build();

            unidadPrivadaRepository.save(unidad);
        }
    }

    @Transactional
    public UnidadPrivada actualizarUnidad(Long id, ActualizarUnidadDto dto) {
        UnidadPrivada unidad = unidadPrivadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada con ID: " + id));

        if (dto.getTorre() != null && !dto.getTorre().isBlank()) {
            unidad.setTorre(dto.getTorre().trim());
        }
        if (dto.getNumeroUnidad() != null && !dto.getNumeroUnidad().isBlank()) {
            unidad.setNumeroUnidad(dto.getNumeroUnidad().trim());
        }
        if (dto.getCoeficiente() != null) {
            unidad.setCoeficiente(dto.getCoeficiente());
        }

        // Actualizar o asignar Propietario
        if (dto.getCedulaPropietario() != null && !dto.getCedulaPropietario().isBlank()) {
            String cedulaProp = dto.getCedulaPropietario().trim();
            Persona propietario = personaRepository.findByCedula(cedulaProp).orElse(null);
            if (propietario == null) {
                propietario = Persona.builder()
                        .cedula(cedulaProp)
                        .nombreCompleto(dto.getNombrePropietario() != null && !dto.getNombrePropietario().isBlank()
                                ? dto.getNombrePropietario().trim()
                                : "Propietario " + cedulaProp)
                        .email(dto.getEmailPropietario() != null && !dto.getEmailPropietario().isBlank()
                                ? dto.getEmailPropietario().trim()
                                : "propietario" + cedulaProp + "@gmail.com")
                        .telefono(dto.getTelefonoPropietario() != null && !dto.getTelefonoPropietario().isBlank()
                                ? dto.getTelefonoPropietario().trim()
                                : "3000000000")
                        .build();
            } else {
                if (dto.getNombrePropietario() != null && !dto.getNombrePropietario().isBlank()) {
                    propietario.setNombreCompleto(dto.getNombrePropietario().trim());
                }
                if (dto.getEmailPropietario() != null && !dto.getEmailPropietario().isBlank()) {
                    propietario.setEmail(dto.getEmailPropietario().trim());
                }
                if (dto.getTelefonoPropietario() != null && !dto.getTelefonoPropietario().isBlank()) {
                    propietario.setTelefono(dto.getTelefonoPropietario().trim());
                }
            }
            propietario = personaRepository.save(propietario);
            unidad.setPropietario(propietario);
        }

        // Actualizar o asignar Apoderado
        if (dto.getDocumentoApoderado() != null) {
            String docApoderado = dto.getDocumentoApoderado().trim();
            if (docApoderado.isBlank()) {
                unidad.setApoderado(null);
                unidad.setPoderAprobado(false);
            } else {
                Usuario apoderado = obtenerOCrearUsuarioApoderado(unidad.getCopropiedad().getId(), docApoderado,
                        dto.getNombreApoderado(), dto.getEmailApoderado(), dto.getTelefonoApoderado());
                if (unidad.getApoderado() == null || !unidad.getApoderado().getDocumento().equals(docApoderado)) {
                    unidad.setApoderado(apoderado);
                    unidad.setPoderAprobado(false); // Requiere aprobación explícita del administrador
                } else {
                    unidad.setApoderado(apoderado);
                }
            }
        }

        UnidadPrivada actualizada = unidadPrivadaRepository.save(unidad);
        return actualizada;
    }

    private Usuario obtenerOCrearUsuarioApoderado(Long copropiedadId, String docApoderado, String nombreCustom,
            String emailCustom, String telCustom) {
        String doc = docApoderado.trim();

        // Reutilizar apoderado si ya existe por documento/cédula
        Usuario usuario = usuarioRepository.findByDocumento(doc).orElse(null);
        if (usuario != null) {
            Copropiedad cop = copropiedadRepository.findById(copropiedadId).orElse(null);
            if (cop != null && usuario.getCopropiedadesAsignadas() != null && !usuario.getCopropiedadesAsignadas().contains(cop)) {
                usuario.getCopropiedadesAsignadas().add(cop);
                usuario = usuarioRepository.save(usuario);
            }
            return usuario;
        }

        String nombre = (nombreCustom != null && !nombreCustom.isBlank()) ? nombreCustom.trim() : "Apoderado " + doc;
        String email = (emailCustom != null && !emailCustom.isBlank()) ? emailCustom.trim()
                : "apoderado." + doc + "@proyectoph.com";
        String tel = (telCustom != null && !telCustom.isBlank()) ? telCustom.trim() : null;

        if (emailCustom != null && !emailCustom.isBlank()) {
            if (usuarioRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException(
                        "El correo electrónico '" + email + "' ya se encuentra registrado para otro usuario.");
            }
        } else {
            if (usuarioRepository.findByEmail(email).isPresent()) {
                email = "apoderado." + doc + "@proyectoph.com";
            }
        }

        Persona personaApoderado = personaRepository.findByCedula(doc).orElse(null);
        if (personaApoderado == null) {
            personaApoderado = personaRepository.save(Persona.builder()
                    .cedula(doc)
                    .nombreCompleto(nombre)
                    .email(email)
                    .telefono(tel)
                    .build());
        }

        String usernameGenerated = usernameService.generarUsernameUnico(nombre);

        Copropiedad copropiedad = copropiedadRepository.findById(copropiedadId).orElse(null);
        Set<Copropiedad> copropiedades = new HashSet<>();
        if (copropiedad != null) {
            copropiedades.add(copropiedad);
        }

        String rawPassApoderado = passwordGeneratorService.generarPasswordSegura();

        usuario = Usuario.builder()
                .persona(personaApoderado)
                .username(usernameGenerated)
                .password(passwordEncoder.encode(rawPassApoderado))
                .roles(new HashSet<>(List.of(Rol.ROLE_APODERADO)))
                .copropiedadesAsignadas(copropiedades)
                .debeCambiarPassword(true)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Registrar la notificación de credenciales iniciales para el apoderado en la
        // tabla de notificaciones
        notificacionService.encolarCredencialesIniciales(
                copropiedadId,
                email,
                nombre,
                usernameGenerated,
                rawPassApoderado,
                "Acceso Apoderado");

        return usuarioGuardado;
    }

    private Persona obtenerOCrearPropietario(Long copropiedadId, String cedulaProp, String torre, String apto,
            String nombreCustom, String emailCustom, String telCustom) {
        String doc = cedulaProp.trim();

        // 1. Buscar si la persona ya existe en la base de datos por su cédula
        Optional<Persona> personaExistenteOpt = personaRepository.findByCedula(doc);
        if (personaExistenteOpt.isPresent()) {
            Persona personaExistente = personaExistenteOpt.get();
            // Asociar la nueva copropiedad al usuario si ya tiene cuenta de acceso
            usuarioRepository.findByDocumento(doc).ifPresent(u -> {
                Copropiedad cop = copropiedadRepository.findById(copropiedadId).orElse(null);
                if (cop != null && u.getCopropiedadesAsignadas() != null && !u.getCopropiedadesAsignadas().contains(cop)) {
                    u.getCopropiedadesAsignadas().add(cop);
                    usuarioRepository.save(u);
                }
            });
            return personaExistente;
        }

        // 2. Si es un nuevo propietario, validar que el correo no esté ocupado por otro usuario o persona
        String nombre = (nombreCustom != null && !nombreCustom.isBlank()) ? nombreCustom.trim()
                : "Propietario T" + torre + "-" + apto;
        String email = (emailCustom != null && !emailCustom.isBlank()) ? emailCustom.trim()
                : "propietario." + doc + "@proyectoph.com";
        String tel = (telCustom != null && !telCustom.isBlank()) ? telCustom.trim() : null;

        if (emailCustom != null && !emailCustom.isBlank()) {
            if (usuarioRepository.findByEmail(email).isPresent() || personaRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException(
                        "El correo electrónico '" + email + "' ya se encuentra registrado para otro usuario.");
            }
        } else {
            if (usuarioRepository.findByEmail(email).isPresent()) {
                email = "propietario." + doc + "." + torre + apto + "@proyectoph.com";
            }
        }

        Persona persona = personaRepository.save(Persona.builder()
                .cedula(doc)
                .nombreCompleto(nombre)
                .email(email)
                .telefono(tel)
                .build());

        Copropiedad copropiedad = copropiedadRepository.findById(copropiedadId).orElse(null);
        Set<Copropiedad> copropiedades = new HashSet<>();
        if (copropiedad != null) {
            copropiedades.add(copropiedad);
        }

        String usernameGen = usernameService.generarUsernameUnico(nombre);
        String rawPassPropietario = passwordGeneratorService.generarPasswordSegura();

        Usuario nuevoUsuario = Usuario.builder()
                .persona(persona)
                .username(usernameGen)
                .password(passwordEncoder.encode(rawPassPropietario))
                .roles(new HashSet<>(List.of(Rol.ROLE_COPROPIETARIO)))
                .copropiedadesAsignadas(copropiedades)
                .debeCambiarPassword(true)
                .build();

        usuarioRepository.save(nuevoUsuario);

        // Encolamiento asíncrono con copropiedadId
        notificacionService.encolarCredencialesIniciales(copropiedadId, email, nombre, usernameGen, rawPassPropietario,
                "Acceso Propietario (Torre " + torre + " - Apto " + apto + ")");

        return persona;
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.isBlank())
            return 5.0;
        try {
            return Double.parseDouble(val.replace(",", "."));
        } catch (NumberFormatException e) {
            return 5.0;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
