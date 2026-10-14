package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.ConsultaResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.ConsultaService;
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
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class ConsultaController {

    private final ConsultaService consultaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<ConsultaResponseDTO>> listarConsultas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long idMedico,
            @RequestParam(required = false) Long idPaciente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            Authentication authentication) {
        List<ConsultaResponseDTO> lista = consultaService.listarConsultasDTO(search, idMedico, idPaciente, fechaDesde, fechaHasta, authentication);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<ConsultaResponseDTO> obtenerConsulta(
            @PathVariable Long id,
            Authentication authentication) {
        ConsultaResponseDTO dto = consultaService.obtenerConsultaDTO(id, authentication);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<ConsultaResponseDTO> registrarConsulta(
            @Valid @RequestBody ConsultaRequestDTO request,
            Authentication authentication) {
        ConsultaResponseDTO creada = consultaService.registrarConsultaDTO(request, authentication);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<ConsultaResponseDTO> actualizarConsulta(
            @PathVariable Long id,
            @Valid @RequestBody ConsultaRequestDTO request,
            Authentication authentication) {
        ConsultaResponseDTO actualizada = consultaService.actualizarConsultaDTO(id, request, authentication);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarConsulta(
            @PathVariable Long id,
            Authentication authentication) {
        consultaService.eliminarConsultaDTO(id, authentication);
        return ResponseEntity.ok(Map.of("message", "Consulta médica eliminada exitosamente"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(Authentication authentication) {
        Map<String, Object> stats = consultaService.obtenerEstadisticas(authentication);
        return ResponseEntity.ok(stats);
    }
}
