package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaHistorialDTO;
import com.example.proyecto_formulas_magistrales.DTO.MedicoRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.MedicoResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioResponseDTO;
import com.example.proyecto_formulas_magistrales.Model.Consulta;
import com.example.proyecto_formulas_magistrales.Model.Medico;
import com.example.proyecto_formulas_magistrales.Model.Usuario;
import com.example.proyecto_formulas_magistrales.Repository.ConsultaRepository;
import com.example.proyecto_formulas_magistrales.Repository.MedicoRepository;
import com.example.proyecto_formulas_magistrales.Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConsultaRepository consultaRepository;
    private final UsuarioService usuarioService;

    // Registrar un nuevo médico
    @Transactional
    public MedicoResponseDTO registrarMedico(MedicoRequestDTO dto) {
        String cleanDni = dto.getDni().trim();
        String cleanColegiatura = dto.getColegiatura().trim();

        if (medicoRepository.existsByDni(cleanDni)) {
            throw new IllegalStateException("El DNI " + cleanDni + " ya está registrado para otro médico");
        }
        if (medicoRepository.existsByColegiatura(cleanColegiatura)) {
            throw new IllegalStateException("El número de colegiatura " + cleanColegiatura + " ya se encuentra registrado");
        }

        Medico nuevo = new Medico();
        mapearDatosMedico(nuevo, dto);
        nuevo.setEstado(dto.getEstado() != null ? dto.getEstado() : true);

        // Vinculación de usuario si se especificó
        if (dto.getIdUsuario() != null) {
            Usuario usuario = validarUsuarioParaMedico(dto.getIdUsuario(), null);
            nuevo.setUsuario(usuario);
        }

        Medico guardado = medicoRepository.save(nuevo);
        return mapToResponseDTO(guardado);
    }

    // Obtener médico por ID
    @Transactional(readOnly = true)
    public MedicoResponseDTO obtenerMedico(Long id) {
        Medico medico = findMedicoById(id);
        return mapToResponseDTO(medico);
    }

    // Listar todos los médicos con filtros de búsqueda, especialidad y estado
    @Transactional(readOnly = true)
    public List<MedicoResponseDTO> listarTodos(String search, String especialidad, Boolean estado) {
        List<Medico> lista = medicoRepository.buscarConFiltros(
                (search != null && !search.isBlank()) ? search.trim() : null,
                especialidad,
                estado
        );
        return lista.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar médico por DNI
    @Transactional(readOnly = true)
    public MedicoResponseDTO buscarPorDni(String dni) {
        Medico medico = medicoRepository.findByDni(dni.trim())
                .orElseThrow(() -> new EntityNotFoundException("Médico con DNI " + dni + " no encontrado"));
        return mapToResponseDTO(medico);
    }

    // Actualizar datos del médico
    @Transactional
    public MedicoResponseDTO actualizarMedico(Long id, MedicoRequestDTO dto) {
        Medico existente = findMedicoById(id);
        String cleanDni = dto.getDni().trim();
        String cleanColegiatura = dto.getColegiatura().trim();

        if (!existente.getDni().equalsIgnoreCase(cleanDni) && medicoRepository.existsByDni(cleanDni)) {
            throw new IllegalStateException("El DNI " + cleanDni + " ya pertenece a otro médico registrado");
        }
        if (!existente.getColegiatura().equalsIgnoreCase(cleanColegiatura) && medicoRepository.existsByColegiatura(cleanColegiatura)) {
            throw new IllegalStateException("El número de colegiatura " + cleanColegiatura + " ya pertenece a otro médico");
        }

        mapearDatosMedico(existente, dto);
        if (dto.getEstado() != null) {
            existente.setEstado(dto.getEstado());
        }

        // Manejo de vinculación de usuario
        Long idUsuarioSolicitado = dto.getIdUsuario();
        Long idUsuarioActual = existente.getUsuario() != null ? existente.getUsuario().getIdUsuario() : null;

        if (idUsuarioSolicitado != null && !idUsuarioSolicitado.equals(idUsuarioActual)) {
            Usuario nuevoUsuario = validarUsuarioParaMedico(idUsuarioSolicitado, id);
            existente.setUsuario(nuevoUsuario);
        } else if (idUsuarioSolicitado == null && idUsuarioActual != null) {
            // Si en el DTO se envía null explícitamente se puede desvincular o mantener;
            // Para evitar desvinculaciones accidentales, si no viene idUsuario y no era requerido, se mantiene
            // sólo si se desea desvincular se podría manejar con un flag, por defecto conservamos.
        }

        Medico actualizado = medicoRepository.save(existente);
        return mapToResponseDTO(actualizado);
    }

    // Desvincular usuario de un médico de forma explícita
    @Transactional
    public MedicoResponseDTO desvincularUsuario(Long idMedico) {
        Medico medico = findMedicoById(idMedico);
        medico.setUsuario(null);
        Medico actualizado = medicoRepository.save(medico);
        return mapToResponseDTO(actualizado);
    }

    // Cambiar estado activo/inactivo (lógico)
    @Transactional
    public void cambiarEstado(Long id, boolean nuevoEstado) {
        Medico medico = findMedicoById(id);
        medico.setEstado(nuevoEstado);
        medicoRepository.save(medico);
    }

    // Eliminar médico físicamente si no posee citas ni consultas clínicas asociadas
    @Transactional
    public void eliminarMedico(Long id) {
        Medico medico = findMedicoById(id);
        if ((medico.getConsultas() != null && !medico.getConsultas().isEmpty()) ||
                (medico.getCitas() != null && !medico.getCitas().isEmpty())) {
            throw new IllegalStateException("No es posible eliminar al médico porque cuenta con citas o consultas clínicas asignadas. Utilice la desactivación lógica en su lugar.");
        }
        medicoRepository.delete(medico);
    }

    // Obtener historial de consultas atendidas por el médico
    @Transactional(readOnly = true)
    public List<ConsultaHistorialDTO> obtenerConsultasMedico(Long idMedico) {
        findMedicoById(idMedico);
        List<Consulta> consultas = consultaRepository.historialMedico(idMedico);

        return consultas.stream().map(c -> {
            String nomPac = c.getPaciente() != null ? (c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos()) : "No especificado";
            String dniPac = c.getPaciente() != null ? c.getPaciente().getDni() : "";
            String nomDoc = c.getMedico() != null ? (c.getMedico().getNombres() + " " + c.getMedico().getApellidos()) : "";

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
                    .nombreMedico(nomDoc)
                    .especialidadMedico(c.getMedico() != null ? c.getMedico().getEspecialidad() : null)
                    .colegiaturaMedico(c.getMedico() != null ? c.getMedico().getColegiatura() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    // Obtener catálogo de especialidades existentes
    @Transactional(readOnly = true)
    public List<String> listarEspecialidades() {
        return medicoRepository.findAllEspecialidades();
    }

    // Listar usuarios con rol MEDICO disponibles para ser vinculados (no vinculados aún a otro médico)
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuariosMedicosDisponibles() {
        return usuarioRepository.findMedicosSinAsignar().stream()
                .map(usuarioService::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Total de médicos registrados
    @Transactional(readOnly = true)
    public long totalMedicos() {
        return medicoRepository.count();
    }

    // Total de médicos activos
    @Transactional(readOnly = true)
    public long totalMedicosActivos() {
        return medicoRepository.findByEstado(true).size();
    }

    private Medico findMedicoById(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Médico con ID " + id + " no encontrado"));
    }

    private Usuario validarUsuarioParaMedico(Long idUsuario, Long idMedicoActual) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("La cuenta de usuario con ID " + idUsuario + " no existe"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalStateException("No se puede vincular una cuenta de usuario inactiva");
        }

        // Verificar que el usuario tenga el rol MEDICO (no vincular cuentas puramente ADMIN)
        boolean tieneRolMedico = usuario.getRoles().stream()
                .anyMatch(r -> "MEDICO".equalsIgnoreCase(r.getNombre()));
        if (!tieneRolMedico) {
            throw new IllegalStateException("El usuario seleccionado ('" + usuario.getUsername() + "') no tiene asignado el rol MEDICO");
        }

        // Verificar que el usuario no esté vinculado ya a otro médico
        Optional<Medico> medicoAsignado = medicoRepository.findByUsuario_IdUsuario(idUsuario);
        if (medicoAsignado.isPresent()) {
            if (idMedicoActual == null || !medicoAsignado.get().getIdMedico().equals(idMedicoActual)) {
                throw new IllegalStateException("El usuario '" + usuario.getUsername() + "' ya está vinculado al Dr.(a) "
                        + medicoAsignado.get().getNombres() + " " + medicoAsignado.get().getApellidos());
            }
        }

        return usuario;
    }

    private void mapearDatosMedico(Medico target, MedicoRequestDTO source) {
        target.setNombres(source.getNombres().trim());
        target.setApellidos(source.getApellidos().trim());
        target.setDni(source.getDni().trim());
        target.setColegiatura(source.getColegiatura().trim());
        target.setEspecialidad(source.getEspecialidad().trim());
        target.setTelefono(source.getTelefono() != null ? source.getTelefono().trim() : null);
        target.setCorreo(source.getCorreo() != null ? source.getCorreo().trim().toLowerCase() : null);
    }

    public MedicoResponseDTO mapToResponseDTO(Medico medico) {
        Long idUsuario = null;
        String username = null;
        String nombreCompleto = null;
        Boolean usuarioActivo = null;

        if (medico.getUsuario() != null) {
            idUsuario = medico.getUsuario().getIdUsuario();
            username = medico.getUsuario().getUsername();
            nombreCompleto = medico.getUsuario().getNombreCompleto();
            usuarioActivo = medico.getUsuario().getActivo();
        }

        int totalConsultas = (medico.getConsultas() != null) ? medico.getConsultas().size() : 0;

        return MedicoResponseDTO.builder()
                .idMedico(medico.getIdMedico())
                .nombres(medico.getNombres())
                .apellidos(medico.getApellidos())
                .dni(medico.getDni())
                .colegiatura(medico.getColegiatura())
                .especialidad(medico.getEspecialidad())
                .telefono(medico.getTelefono())
                .correo(medico.getCorreo())
                .estado(medico.getEstado())
                .idUsuario(idUsuario)
                .username(username)
                .nombreCompletoUsuario(nombreCompleto)
                .usuarioActivo(usuarioActivo)
                .totalConsultas(totalConsultas)
                .build();
    }
}
