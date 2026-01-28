package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.UsuarioServicio;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioControlador {

    private static final Logger log = LoggerFactory.getLogger(UsuarioControlador.class);
    
    private final UsuarioServicio usuarioServicio;
    private static List<String> ROLES_DISPONIBLES = List.of("ADMIN", "DEV", "CAJERO");

    // SOLO ADMIN y DEV pueden acceder
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @GetMapping
    public String listarUsuarios(Model model) {
        log.info("📋 GET /admin/usuarios - Listando usuarios");
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());
        model.addAttribute("roles", ROLES_DISPONIBLES);
        return "usuarios/lista";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        log.info("📝 GET /admin/usuarios/nuevo - Mostrando formulario crear usuario");
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", ROLES_DISPONIBLES);
        model.addAttribute("modo", "crear");
        return "usuarios/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(required = false) String confirmarPassword,
            RedirectAttributes redirect) {
        
        log.info("💾 POST /admin/usuarios/guardar - Creando usuario: {}", usuario.getNombreUsuario());
        log.debug("Datos usuario - Rol: {}, Activo: {}", usuario.getRol(), usuario.isActivo());

        // Validar que las contraseñas coincidan
        if (!usuario.getContrasenna().equals(confirmarPassword)) {
            log.warn("❌ Contraseñas no coinciden para usuario: {}", usuario.getNombreUsuario());
            redirect.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/admin/usuarios/nuevo";
        }

        // RESTRICCIÓN: Solo se pueden crear ADMIN y CAJERO
        if ("DEV".equals(usuario.getRol())) {
            // Obtener usuario actual y verificar si es DEV
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DEV"))) {
                log.warn("⚠️ Intento de crear usuario DEV por usuario no autorizado: {}", auth.getName());
                redirect.addFlashAttribute("error", "Solo los usuarios DEV pueden crear otros DEV");
                return "redirect:/admin/usuarios/nuevo";
            }
        }

        usuarioServicio.crearUsuario(usuario);
        log.info("✅ Usuario creado exitosamente: {} (ID: {})", usuario.getNombreUsuario(), usuario.getId());
        redirect.addFlashAttribute("success", "Usuario creado exitosamente");
        return "redirect:/admin/usuarios";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        log.info("✏️ GET /admin/usuarios/editar/{} - Editando usuario", id);
        model.addAttribute("usuario", usuarioServicio.obtenerUsuario(id));
        model.addAttribute("roles", ROLES_DISPONIBLES);
        model.addAttribute("modo", "editar");
        return "usuarios/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @PostMapping("/actualizar/{id}")
    public String actualizarUsuario(@PathVariable Long id,
            @ModelAttribute Usuario usuario,
            @RequestParam(required = false) String contrasenna,
            @RequestParam(required = false) String confirmarPassword,
            RedirectAttributes redirect) {
        
        log.info("🔄 POST /admin/usuarios/actualizar/{} - Actualizando usuario", id);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Usuario autenticado: {}, Roles: {}", auth.getName(), auth.getAuthorities());

        // Validar contraseñas si se van a cambiar
        if (contrasenna != null && !contrasenna.isEmpty()) {
            if (!contrasenna.equals(confirmarPassword)) {
                log.warn("❌ Contraseñas no coinciden al actualizar usuario ID: {}", id);
                redirect.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:/admin/usuarios/editar/" + id;
            }
            usuario.setContrasenna(contrasenna);
            log.debug("Contraseña actualizada para usuario ID: {}", id);
        } else {
            usuario.setContrasenna(null); // No cambiar password
            log.debug("Contraseña no cambiada para usuario ID: {}", id);
        }

        // RESTRICCIÓN: Solo ADMIN y DEV pueden cambiar roles, con limitaciones
        boolean esDev = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DEV"));
        boolean esAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // Obtener usuario actual para validaciones
        Usuario usuarioActual = usuarioServicio.obtenerUsuario(id);

        // Solo DEV puede asignar rol DEV
        if ("DEV".equals(usuario.getRol()) && !esDev) { // Cambio aquí
            log.warn("⚠️ Intento de asignar rol DEV por usuario no autorizado: {}", auth.getName());
            redirect.addFlashAttribute("error", "Solo usuarios DEV pueden asignar rol DEV");
            return "redirect:/admin/usuarios/editar/" + id;
        }

        usuarioServicio.actualizarUsuario(id, usuario);
        log.info("✅ Usuario actualizado exitosamente ID: {} - Nuevo rol: {}", id, usuario.getRol());
        redirect.addFlashAttribute("success", "Usuario actualizado exitosamente");
        return "redirect:/admin/usuarios";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    @PostMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirect) {
        log.info("🗑️ POST /admin/usuarios/eliminar/{} - Eliminando usuario", id);
        
        Usuario usuario = usuarioServicio.obtenerUsuario(id);
        usuarioServicio.eliminarUsuario(id);
        
        log.info("✅ Usuario desactivado: {} (ID: {})", usuario.getNombreUsuario(), id);
        redirect.addFlashAttribute("success", "Usuario desactivado exitosamente");
        return "redirect:/admin/usuarios";
    }

}