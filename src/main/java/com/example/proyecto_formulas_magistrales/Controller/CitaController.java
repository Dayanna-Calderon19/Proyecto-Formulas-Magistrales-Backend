package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.CitaReprogramarDTO;
import com.example.proyecto_formulas_magistrales.DTO.CitaRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.CitaResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<CitaResponseDTO>> listarCitas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long idMedico,
            @RequestParam(required = false) Long idPaciente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            Authentication authentication) {
        List<CitaResponseDTO> lista = citaService.listarCitasDTO(search, estado, idMedico, idPaciente, fechaDesde, fechaHasta, authentication);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> obtenerCita(
            @PathVariable Long id,
            Authentication authentication) {
        CitaResponseDTO dto = citaService.obtenerCitaDTO(id, authentication);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/disponibles-consulta")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<CitaResponseDTO>> listarCitasDisponiblesConsulta(Authentication authentication) {
        List<CitaResponseDTO> disponibles = citaService.listarCitasDisponiblesParaConsulta(authentication);
        return ResponseEntity.ok(disponibles);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> registrarCita(
            @Valid @RequestBody CitaRequestDTO request,
            Authentication authentication) {
        CitaResponseDTO creada = citaService.registrarCitaDTO(request, authentication);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> actualizarCita(
            @PathVariable Long id,
            @Valid @RequestBody CitaRequestDTO request,
            Authentication authentication) {
        CitaResponseDTO actualizada = citaService.actualizarCitaDTO(id, request, authentication);
        return ResponseEntity.ok(actualizada);
    }

    @PatchMapping("/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> reprogramarCita(
            @PathVariable Long id,
            @Valid @RequestBody CitaReprogramarDTO request,
            Authentication authentication) {
        CitaResponseDTO reprogramada = citaService.reprogramarCitaDTO(id, request, authentication);
        return ResponseEntity.ok(reprogramada);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> cancelarCita(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        CitaResponseDTO cancelada = citaService.cancelarCitaDTO(id, motivo, authentication);
        return ResponseEntity.ok(cancelada);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<CitaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado,
            Authentication authentication) {
        CitaResponseDTO actualizada = citaService.cambiarEstadoDTO(id, nuevoEstado, authentication);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarCita(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.ok(Map.of("message", "Cita médica eliminada exitosamente del sistema"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(Authentication authentication) {
        Map<String, Object> stats = citaService.obtenerEstadisticas(authentication);
        return ResponseEntity.ok(stats);
    }
}
