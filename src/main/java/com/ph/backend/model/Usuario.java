package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "persona_id", referencedColumnName = "id")
    private Persona persona;

    @Column(unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    public String getDocumento() {
        return persona != null ? persona.getCedula() : null;
    }

    public String getNombreCompleto() {
        return persona != null ? persona.getNombreCompleto() : null;
    }

    public String getEmail() {
        return persona != null ? persona.getEmail() : null;
    }

    public String getTelefono() {
        return persona != null ? persona.getTelefono() : null;
    }

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_copropiedades",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "copropiedad_id"),
        indexes = {@Index(name = "idx_uc_copropiedad_id", columnList = "copropiedad_id")}
    )
    @Builder.Default
    private Set<Copropiedad> copropiedadesAsignadas = new HashSet<>();

    @JsonIgnore
    @Column(name = "clave_otp")
    private String claveOtp;

    @Column(name = "fecha_expiracion_otp")
    private LocalDateTime fechaExpiracionOtp;

    @Column(name = "debe_cambiar_password", nullable = false)
    @Builder.Default
    private Boolean debeCambiarPassword = true;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

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
