package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaResponseDTO {

    private Long idConsulta;
    private LocalDateTime fechaConsulta;
    private String motivo;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;

    // Cita asociada
    private Long idCita;
    private LocalDateTime fechaHoraCita;

    // Paciente
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;
    private String telefonoPaciente;
    private String correoPaciente;

    // Médico
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
    private String colegiaturaMedico;

    // Fórmulas magistrales vinculadas
    @Builder.Default
    private int totalFormulas = 0;

    @Builder.Default
    private List<FormulaResumenDTO> formulas = new ArrayList<>();
}
