package com.example.proyecto_formulas_magistrales;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProyectoFormulasMagistralesApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private com.example.proyecto_formulas_magistrales.Service.UsuarioService usuarioService;

	@org.springframework.beans.factory.annotation.Autowired
	private com.example.proyecto_formulas_magistrales.Service.RolService rolService;

	@Test
	void contextLoads() {
		long start = System.currentTimeMillis();
		var roles = rolService.listarTodos();
		System.out.println(">>> ROLES OBTENIDOS EN " + (System.currentTimeMillis() - start) + "ms. Cantidad: " + roles.size());

		start = System.currentTimeMillis();
		var usuarios = usuarioService.listarTodos(null, null, null);
		System.out.println(">>> USUARIOS OBTENIDOS EN " + (System.currentTimeMillis() - start) + "ms. Cantidad: " + usuarios.size());
	}


}
