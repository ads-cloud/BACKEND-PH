package com.ph.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "config_tareas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_tarea", nullable = false, unique = true)
    private String nombreTarea;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "intervalo_segundos")
    @Builder.Default
    private Integer intervaloSegundos = 15;

    private String descripcion;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
