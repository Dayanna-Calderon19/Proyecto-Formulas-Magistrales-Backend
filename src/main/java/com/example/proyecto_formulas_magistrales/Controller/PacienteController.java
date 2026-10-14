package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaHistorialDTO;
import com.example.proyecto_formulas_magistrales.DTO.PacienteRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.PacienteResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    // Listar pacientes con búsqueda y filtro de estado (ADMIN y MEDICO)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<PacienteResponseDTO>> listarPacientes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean estado) {
        return ResponseEntity.ok(pacienteService.listarTodos(search, estado));
    }

    // Obtener paciente por ID (ADMIN y MEDICO)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<PacienteResponseDTO> obtenerPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPaciente(id));
    }

    // Buscar paciente por DNI (ADMIN y MEDICO)
    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<PacienteResponseDTO> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(pacienteService.buscarPorDni(dni));
    }

    // Obtener historial de consultas clínicas del paciente (ADMIN y MEDICO)
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<ConsultaHistorialDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerHistorialConsultas(id));
    }

    // Registrar un nuevo paciente (ADMIN y MEDICO)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<PacienteResponseDTO> registrarPaciente(@Valid @RequestBody PacienteRequestDTO dto) {
        PacienteResponseDTO creado = pacienteService.registrarPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Actualizar datos del paciente (ADMIN y MEDICO)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<PacienteResponseDTO> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDTO dto) {
        PacienteResponseDTO actualizado = pacienteService.actualizarPaciente(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    // Activar paciente (solo ADMIN)
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activarPaciente(@PathVariable Long id) {
        pacienteService.cambiarEstado(id, true);
        return ResponseEntity.ok(Collections.singletonMap("message", "Paciente activado correctamente"));
    }

    // Desactivar paciente - baja lógica (solo ADMIN)
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> desactivarPaciente(@PathVariable Long id) {
        pacienteService.cambiarEstado(id, false);
        return ResponseEntity.ok(Collections.singletonMap("message", "Paciente desactivado correctamente"));
    }

    // Eliminar paciente físicamente (solo ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarPaciente(@PathVariable Long id) {
        pacienteService.eliminarPaciente(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Paciente eliminado exitosamente del sistema"));
    }

    // Estadísticas
    @GetMapping("/stats/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Long>> totalPacientes() {
        return ResponseEntity.ok(Collections.singletonMap("total", pacienteService.totalPacientes()));
    }

    @GetMapping("/stats/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Long>> totalPacientesActivos() {
        return ResponseEntity.ok(Collections.singletonMap("totalActivos", pacienteService.totalPacientesActivos()));
    }
}
