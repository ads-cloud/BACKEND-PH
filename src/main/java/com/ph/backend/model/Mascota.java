package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mascotas", indexes = {
    @Index(name = "idx_mascota_unidad_id", columnList = "unidad_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadPrivada unidad;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo; // PERRO, GATO, AVE, OTRO

    private String raza;

    private String color;

    @Builder.Default
    @Column(name = "vacunas_al_dia", nullable = false)
    private Boolean vacunasAlDia = true;

    @Column(length = 500)
    private String observaciones;
}
