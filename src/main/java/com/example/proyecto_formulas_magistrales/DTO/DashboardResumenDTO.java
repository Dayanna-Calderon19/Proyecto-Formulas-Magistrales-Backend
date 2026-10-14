package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumenDTO {
    private String rol;
    private String username;
    private String nombreUsuario;
    private boolean tienePerfilMedico;
    private DashboardMedicoInfoDTO medicoInfo;
    private String periodo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    private List<DashboardKpiDTO> kpis;
    private List<DashboardEstadoCountDTO> citasPorEstado;
    private List<DashboardSeriePuntoDTO> tendenciaPeriodo;
    private List<DashboardEstadoCountDTO> formulasPorEstado;
    private List<DashboardProximaCitaDTO> proximasCitas;
}
