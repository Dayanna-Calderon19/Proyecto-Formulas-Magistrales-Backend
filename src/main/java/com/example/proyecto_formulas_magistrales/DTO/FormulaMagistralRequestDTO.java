package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaMagistralRequestDTO {

    @NotBlank(message = "El nombre de la fórmula es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
    private String descripcion;

    @NotBlank(message = "La composición o ingredientes son obligatorios")
    @Size(max = 255, message = "La composición no puede exceder los 255 caracteres")
    private String composicion;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "1.00", message = "La cantidad debe ser igual o mayor a 1.00")
    private BigDecimal cantidad;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(min = 1, max = 30, message = "La unidad de medida no puede exceder 30 caracteres")
    private String unidad;

    @NotNull(message = "La fecha de preparación es obligatoria")
    private LocalDate fechaPreparacion;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    @NotBlank(message = "El estado de la fórmula es obligatorio")
    @Size(max = 25, message = "El estado no puede exceder 25 caracteres")
    @Builder.Default
    private String estado = "ACTIVO";

    // Identificador opcional de la consulta médica asociada
    private Long idConsulta;
}
