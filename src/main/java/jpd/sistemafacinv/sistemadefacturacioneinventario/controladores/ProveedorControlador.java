package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Proveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ProveedorServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/proveedores")
@AllArgsConstructor
public class ProveedorControlador {

    private static final Logger log = LoggerFactory.getLogger(ProveedorControlador.class);
    
    private final ProveedorServicio proveedorServicio;
    

    @GetMapping
    public String listar(Model modelo) {
        log.info("📋 GET /proveedores - Listando proveedores");
        modelo.addAttribute("proveedores", proveedorServicio.listarProveedores());
        return "proveedores/lista";
    }
    
    @GetMapping("/nuevo")
    public String nuevoProveedor(Model modelo) {
        log.info("📝 GET /proveedores/nuevo - Mostrando formulario nuevo proveedor");
        modelo.addAttribute("proveedor", new Proveedor());
        return "proveedores/formulario";
    }
    
    @GetMapping("/editar/{id}")
    public String editarProveedor(@PathVariable Integer id, Model modelo) {
        log.info("✏️ GET /proveedores/editar/{} - Editando proveedor", id);
        modelo.addAttribute("proveedor", proveedorServicio.buscarProveedor(id));
        return "proveedores/formulario";
    }
    
    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Integer id, Model modelo) {
        log.info("👁️ GET /proveedores/detalle/{} - Viendo detalle proveedor", id);
        modelo.addAttribute("proveedor", proveedorServicio.buscarProveedor(id));
        return "proveedores/detalle";
    }
    
    @PostMapping("/guardar")
    public String guardarProveedor(@ModelAttribute Proveedor proveedor) {
        log.info("💾 POST /proveedores/guardar - Guardando proveedor: {}", proveedor.getNombre());
        proveedorServicio.guardarProveedor(proveedor);
        log.info("✅ Proveedor guardado exitosamente: {} (ID: {})", proveedor.getNombre(), proveedor.getId());
        return "redirect:/proveedores";
    }
    
    @PostMapping("/editar/{id}")
    public String actualizarProveedor(@PathVariable Integer id, @ModelAttribute Proveedor proveedor) {
        log.info("🔄 POST /proveedores/editar/{} - Actualizando proveedor", id);
        proveedorServicio.actualizarProveedor(id, proveedor);
        log.info("✅ Proveedor actualizado exitosamente ID: {}", id);
        return "redirect:/proveedores";
    }
}