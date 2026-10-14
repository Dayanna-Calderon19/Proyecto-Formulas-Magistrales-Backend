package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaMagistralResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String composicion;
    private BigDecimal cantidad;
    private String unidad;
    private LocalDate fechaPreparacion;
    private LocalDate fechaVencimiento;
    private String estado;

    // Datos de la consulta médica asociada (cuando existe)
    private Long idConsulta;
    private LocalDateTime fechaConsulta;
    private String motivoConsulta;
    private String diagnosticoConsulta;

    // Datos del paciente derivado
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;

    // Datos del médico responsable
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
    private String colegiaturaMedico;
}
