package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.EmpresaClienteDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.SuperAdminService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/superadmin")
@AllArgsConstructor
@Slf4j
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final EmpresaRepositorio empresaRepositorio;

    @GetMapping("/login")
    public String mostrarLogin() {
        log.info("🔓 Mostrando página de login para SUPER_ADMIN");
        return "superadmin/login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        log.warn("⛔ Intento de acceso no autorizado a sección SUPER_ADMIN");
        return "superadmin/acceso-denegado";
    }

    @GetMapping("/empresas")
    public String listarEmpresas(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("nombreUsuario", principal.getName());
        }

        // Get all companies
        List<Empresa> empresas = empresaRepositorio.findAll();
        model.addAttribute("empresas", empresas);
        model.addAttribute("totalEmpresas", empresas.size());

        // Count active/inactive
        long conteo = empresas.stream()
                .count();
        model.addAttribute("empresasActivas", conteo);

        return "superadmin/empresas/lista";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("📊 Accediendo al dashboard SUPER_ADMIN");
        return "redirect:/superadmin/empresas";
    }

    @GetMapping("/empresas/nueva")
    public String mostrarFormulario(Model model) {
        log.info("📄 [CONTROLLER] Mostrando formulario para crear nueva empresa");
        log.debug("🔧 Preparando modelo con nuevo EmpresaClienteDTO");
        model.addAttribute("empresaDTO", new EmpresaClienteDTO());
        log.info("✅ Formulario listo para mostrar");
        return "superadmin/empresas/form";
    }

    @PostMapping("/empresas/crear")
    public String crearEmpresa(@ModelAttribute EmpresaClienteDTO dto,
            Model model) {
        log.info("📨 [CONTROLLER] Recibiendo solicitud para crear nueva empresa");
        log.debug("📋 Datos recibidos: Nombre={}, Subdominio={}, Email={}, Plan={}",
                dto.getNombre(), dto.getSubdominio(), dto.getEmailContacto(), dto.getPlan());
        log.debug("⚙️ Configuración: crearUsuarios={}, crearDatosIniciales={}",
                dto.isCrearUsuarios(), dto.isCrearDatosIniciales());

        try {
            log.info("🔄 Delegando creación al SuperAdminService...");
            Empresa empresa = superAdminService.crearEmpresaCliente(dto);

            log.info("✅ Empresa creada exitosamente - ID: {}, Nombre: {}",
                    empresa.getId(), empresa.getNombre());
            log.info("📊 Datos empresa creada: Subdominio={}, Email={}",
                    empresa.getSubdominio(), empresa.getEmailContacto());

            model.addAttribute("exito", "Empresa creada exitosamente");
            model.addAttribute("empresa", empresa);

            log.info("🎯 Redirigiendo a página de éxito");
            return "redirect:/superadmin/empresas"; 

        } catch (RuntimeException e) {
            log.error("❌ Error al crear empresa: {}", e.getMessage());
            log.debug("📋 Datos fallidos que causaron el error: {}", dto);

            model.addAttribute("error", e.getMessage());
            model.addAttribute("empresaDTO", dto);

            log.info("↩️  Redirigiendo de vuelta al formulario con error");
            return "superadmin/empresas/form";
        }
    }
}