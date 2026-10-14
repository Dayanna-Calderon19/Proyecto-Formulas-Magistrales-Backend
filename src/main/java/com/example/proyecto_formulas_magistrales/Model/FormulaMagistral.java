package com.example.proyecto_formulas_magistrales.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="formulas_magistrales")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FormulaMagistral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ingresa el nombre de la fórmula")
    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "Ingresa la composición")
    @Column(name = "composicion", columnDefinition = "TEXT", nullable = false)
    private String composicion;

    @NotNull(message = "Ingresa la cantidad")
    @DecimalMin(value = "1.00", message = "La cantidad debe ser igual o mayor a 1.00")
    @Column(name = "cantidad", precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    @NotBlank(message = "Ingresa la unidad de medida")
    @Column(name = "unidad", length = 30, nullable = false)
    private String unidad;

    @NotNull(message = "Ingresa la fecha de preparación")
    @Column(name = "fecha_preparacion", nullable = false)
    private LocalDate fechaPreparacion;

    @NotNull(message = "Ingresa la fecha de vencimiento")
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotBlank(message = "Ingresa el estado")
    @Column(name = "estado", length = 25, nullable = false)
    private String estado = "ACTIVO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consulta")
    private Consulta consulta;
}
