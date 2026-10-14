package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.DashboardResumenDTO;
import com.example.proyecto_formulas_magistrales.Service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenDTO> obtenerResumen(
            @RequestParam(value = "periodo", defaultValue = "30_DIAS") String periodo,
            @RequestParam(value = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication auth) {

        DashboardResumenDTO resumen = dashboardService.obtenerResumen(periodo, fechaDesde, fechaHasta, auth);
        return ResponseEntity.ok(resumen);
    }
}
