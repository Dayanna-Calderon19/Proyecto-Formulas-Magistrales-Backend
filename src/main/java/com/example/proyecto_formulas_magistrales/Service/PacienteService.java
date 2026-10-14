package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaHistorialDTO;
import com.example.proyecto_formulas_magistrales.DTO.PacienteRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.PacienteResponseDTO;
import com.example.proyecto_formulas_magistrales.Model.Consulta;
import com.example.proyecto_formulas_magistrales.Model.Paciente;
import com.example.proyecto_formulas_magistrales.Repository.ConsultaRepository;
import com.example.proyecto_formulas_magistrales.Repository.PacienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;

    // Registrar un nuevo paciente
    @Transactional
    public PacienteResponseDTO registrarPaciente(PacienteRequestDTO dto) {
        String cleanDni = dto.getDni().trim();
        if (pacienteRepository.existsByDni(cleanDni)) {
            throw new IllegalStateException("El DNI " + cleanDni + " ya se encuentra registrado en el sistema");
        }

        Paciente paciente = new Paciente();
        mapearDatosPaciente(paciente, dto);
        paciente.setEstado(dto.getEstado() != null ? dto.getEstado() : true);

        Paciente guardado = pacienteRepository.save(paciente);
        return mapToResponseDTO(guardado);
    }

    // Obtener paciente por ID
    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPaciente(Long id) {
        Paciente paciente = findPacienteById(id);
        return mapToResponseDTO(paciente);
    }

    // Listar pacientes con filtros de búsqueda y estado
    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarTodos(String search, Boolean estado) {
        List<Paciente> lista = pacienteRepository.buscarConFiltros(
                (search != null && !search.isBlank()) ? search.trim() : null,
                estado
        );
        return lista.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar paciente por DNI
    @Transactional(readOnly = true)
    public PacienteResponseDTO buscarPorDni(String dni) {
        Paciente paciente = pacienteRepository.findByDni(dni.trim())
                .orElseThrow(() -> new EntityNotFoundException("Paciente con DNI " + dni + " no encontrado"));
        return mapToResponseDTO(paciente);
    }

    // Actualizar datos del paciente
    @Transactional
    public PacienteResponseDTO actualizarPaciente(Long id, PacienteRequestDTO dto) {
        Paciente existente = findPacienteById(id);
        String cleanDni = dto.getDni().trim();

        if (!existente.getDni().equalsIgnoreCase(cleanDni) && pacienteRepository.existsByDni(cleanDni)) {
            throw new IllegalStateException("El DNI " + cleanDni + " ya pertenece a otro paciente registrado");
        }

        mapearDatosPaciente(existente, dto);
        if (dto.getEstado() != null) {
            existente.setEstado(dto.getEstado());
        }

        Paciente actualizado = pacienteRepository.save(existente);
        return mapToResponseDTO(actualizado);
    }

    // Cambiar estado activo/inactivo (desactivación lógica para preservar historial)
    @Transactional
    public void cambiarEstado(Long id, boolean nuevoEstado) {
        Paciente paciente = findPacienteById(id);
        paciente.setEstado(nuevoEstado);
        pacienteRepository.save(paciente);
    }

    // Eliminar paciente físicamente del sistema si no posee citas o consultas clínicas asociadas
    @Transactional
    public void eliminarPaciente(Long id) {
        Paciente paciente = findPacienteById(id);
        if ((paciente.getConsultas() != null && !paciente.getConsultas().isEmpty()) ||
                (paciente.getCitas() != null && !paciente.getCitas().isEmpty())) {
            throw new IllegalStateException("No es posible eliminar al paciente porque cuenta con citas o consultas clínicas asociadas. Utilice la desactivación lógica en su lugar.");
        }
        pacienteRepository.delete(paciente);
    }

    // Obtener historial de consultas médicas de un paciente
    @Transactional(readOnly = true)
    public List<ConsultaHistorialDTO> obtenerHistorialConsultas(Long idPaciente) {
        // Validar que el paciente exista
        findPacienteById(idPaciente);

        List<Consulta> consultas = consultaRepository.historialPaciente(idPaciente);
        return consultas.stream().map(c -> {
            String nombreDoc = c.getMedico() != null ? (c.getMedico().getNombres() + " " + c.getMedico().getApellidos()) : "No asignado";
            String espDoc = c.getMedico() != null ? c.getMedico().getEspecialidad() : "";
            String colDoc = c.getMedico() != null ? c.getMedico().getColegiatura() : "";
            String nomPac = c.getPaciente() != null ? (c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos()) : "";
            String dniPac = c.getPaciente() != null ? c.getPaciente().getDni() : "";

            return ConsultaHistorialDTO.builder()
                    .idConsulta(c.getIdConsulta())
                    .fechaConsulta(c.getFechaConsulta())
                    .motivo(c.getMotivo())
                    .diagnostico(c.getDiagnostico())
                    .tratamiento(c.getTratamiento())
                    .observaciones(c.getObservaciones())
                    .idPaciente(c.getPaciente() != null ? c.getPaciente().getIdPaciente() : null)
                    .nombrePaciente(nomPac)
                    .dniPaciente(dniPac)
                    .idMedico(c.getMedico() != null ? c.getMedico().getIdMedico() : null)
                    .nombreMedico(nombreDoc)
                    .especialidadMedico(espDoc)
                    .colegiaturaMedico(colDoc)
                    .build();
        }).collect(Collectors.toList());
    }

    // Total de pacientes registrados
    @Transactional(readOnly = true)
    public long totalPacientes() {
        return pacienteRepository.count();
    }

    // Total de pacientes activos
    @Transactional(readOnly = true)
    public long totalPacientesActivos() {
        return pacienteRepository.findByEstado(true).size();
    }

    private Paciente findPacienteById(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Paciente con ID " + id + " no encontrado"));
    }

    private void mapearDatosPaciente(Paciente target, PacienteRequestDTO source) {
        target.setNombres(source.getNombres().trim());
        target.setApellidos(source.getApellidos().trim());
        target.setDni(source.getDni().trim());
        target.setFechaNacimiento(source.getFechaNacimiento());
        target.setSexo(source.getSexo() != null ? source.getSexo().trim() : null);
        target.setTelefono(source.getTelefono() != null ? source.getTelefono().trim() : null);
        target.setCorreo(source.getCorreo() != null ? source.getCorreo().trim().toLowerCase() : null);
        target.setDireccion(source.getDireccion() != null ? source.getDireccion().trim() : null);
    }

    public PacienteResponseDTO mapToResponseDTO(Paciente paciente) {
        int numConsultas = (paciente.getConsultas() != null) ? paciente.getConsultas().size() : 0;

        return PacienteResponseDTO.builder()
                .idPaciente(paciente.getIdPaciente())
                .nombres(paciente.getNombres())
                .apellidos(paciente.getApellidos())
                .dni(paciente.getDni())
                .fechaNacimiento(paciente.getFechaNacimiento())
                .sexo(paciente.getSexo())
                .telefono(paciente.getTelefono())
                .correo(paciente.getCorreo())
                .direccion(paciente.getDireccion())
                .estado(paciente.getEstado())
                .totalConsultas(numConsultas)
                .build();
    }
}
