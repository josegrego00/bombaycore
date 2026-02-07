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

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.IngredienteServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/ingredientes")
@AllArgsConstructor
public class IngredienteControlador {

    private static final Logger log = LoggerFactory.getLogger(IngredienteControlador.class);

    private final IngredienteServicio ingredienteServicio;

    @GetMapping
    public String listarIngredientes(Model model) {
        log.info("📋 GET /ingredientes - Listando ingredientes");
        model.addAttribute("ingredientes", ingredienteServicio.listaIngredientes());
        return "ingredientes/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("📝 GET /ingredientes/nuevo - Mostrando formulario nuevo ingrediente");
        model.addAttribute("ingrediente", new Ingrediente());
        model.addAttribute("titulo", "Nuevo Ingrediente");
        return "ingredientes/formulario";
    }

    @PostMapping("/guardar")
    public String guardarIngrediente(@ModelAttribute Ingrediente ingrediente, Model model) {
        log.info("💾 POST /ingredientes/guardar - Guardando ingrediente: {}", ingrediente.getNombre());

        try {

            ingredienteServicio.crearIngrediente(ingrediente);
            log.info("✅ Ingrediente guardado exitosamente: {} (ID: {})", ingrediente.getNombre(), ingrediente.getId());
            return "redirect:/ingredientes";

        } catch (RuntimeException e) {
            log.warn("Error al guardar ingrediente: {}", e.getMessage());

            // Mantener los datos ingresados para que el usuario no tenga que reescribir
            model.addAttribute("ingrediente", ingrediente);
            model.addAttribute("error", e.getMessage());

            return "ingredientes/formulario"; // vuelve al formulario
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id, Model model) {
        log.info("✏️ GET /ingredientes/editar/{} - Editando ingrediente", id);
        Ingrediente ingrediente = ingredienteServicio.buscarIngrediente(id);
        model.addAttribute("ingrediente", ingrediente);
        model.addAttribute("titulo", "Editar Ingrediente");
        return "ingredientes/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarIngrediente(@PathVariable int id) {
        log.info("🗑️ GET /ingredientes/eliminar/{} - Eliminando ingrediente", id);
        ingredienteServicio.eliminarIngrediente(id);
        log.info("✅ Ingrediente eliminado ID: {}", id);
        return "redirect:/ingredientes";
    }
}