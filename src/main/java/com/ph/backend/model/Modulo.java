package com.ph.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "modulos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String icono;

    @Column(nullable = false, length = 100)
    private String ruta;

    @Column(name = "color_gradient", length = 100)
    private String colorGradient;

    @Column(name = "border_color", length = 100)
    private String borderColor;

    @Column(name = "shadow_color", length = 100)
    private String shadowColor;

    @Column(length = 50)
    private String badge;

    @Column(name = "badge_color", length = 100)
    private String badgeColor;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "modulo_roles", joinColumns = @JoinColumn(name = "modulo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    @Builder.Default
    private Set<Rol> rolesPermitidos = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
