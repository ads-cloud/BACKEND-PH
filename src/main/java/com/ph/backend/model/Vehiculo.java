package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehiculos", indexes = {
    @Index(name = "idx_vehiculo_unidad_id", columnList = "unidad_id"),
    @Index(name = "idx_vehiculo_placa", columnList = "placa")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadPrivada unidad;

    @Column(nullable = false)
    private String placa;

    @Column(nullable = false)
    private String tipo; // CARRO, MOTO, BICICLETA, OTRO

    @Column(name = "marca_modelo")
    private String marcaModelo;

    private String color;

    private String parqueadero; // Ej: Celda 102, P2-15

    @Column(length = 500)
    private String observaciones;
}
