package com.example.proyecto_formulas_magistrales.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medico")
    private Long idMedico;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @NotBlank
    @Size(max = 15)
    @Column(name = "dni", length = 15, nullable = false, unique = true)
    private String dni;

    @NotBlank
    @Size(max = 30)
    @Column(name = "colegiatura", length = 30, nullable = false, unique = true)
    private String colegiatura;

    @NotBlank
    @Size(max = 100)
    @Column(name = "especialidad", length = 100, nullable = false)
    private String especialidad;

    @Size(max = 20)
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 150)
    @Email
    @Column(name = "correo", length = 150)
    private String correo;

    @NotNull
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @OneToOne
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "medico", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<Cita> citas = new ArrayList<>();

    @OneToMany(mappedBy = "medico", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<Consulta> consultas = new ArrayList<>();



    // Getters and Setters
    public Long getIdMedico() { return idMedico; }
    public void setIdMedico(Long idMedico) { this.idMedico = idMedico; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getColegiatura() { return colegiatura; }
    public void setColegiatura(String colegiatura) { this.colegiatura = colegiatura; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public List<Cita> getCitas() { return citas; }
    public void setCitas(List<Cita> citas) { this.citas = citas; }
    public List<Consulta> getConsultas() { return consultas; }
    public void setConsultas(List<Consulta> consultas) { this.consultas = consultas; }
}
