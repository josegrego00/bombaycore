package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.RecetaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.IngredienteServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.RecetaServicio;
import lombok.AllArgsConstructor;

// controladores/RecetaControlador.java
@Controller
@RequestMapping("/recetas")
@AllArgsConstructor
public class RecetaControlador {

    private static final Logger log = LoggerFactory.getLogger(RecetaControlador.class);
    
    private final RecetaServicio recetaServicio;
    private final IngredienteServicio ingredienteServicio;

    @GetMapping
    public String listarRecetas(Model model) {
        log.info("📋 GET /recetas - Listando recetas");
        model.addAttribute("recetas", recetaServicio.listaRecetas());
        return "recetas/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("📝 GET /recetas/nuevo - Mostrando formulario nueva receta");
        model.addAttribute("receta", new Receta());
        model.addAttribute("ingredientes", ingredienteServicio.listaIngredientes());
        model.addAttribute("titulo", "Nueva Receta");
        return "recetas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarReceta(@RequestParam(required = false) Integer id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam(value = "ingredienteIds", required = false) List<Integer> ingredienteIds,
            @RequestParam(value = "cantidades", required = false) List<Double> cantidades) {

        log.info("💾 POST /recetas/guardar - Guardando receta. ID: {}, Nombre: {}", id, nombre);
        log.debug("Ingredientes recibidos: {}, Cantidades: {}", 
                 ingredienteIds != null ? ingredienteIds.size() : 0, 
                 cantidades != null ? cantidades.size() : 0);

        if (id != null && id > 0) {
            // Usar el nuevo método del servicio
            log.debug("Actualizando receta existente ID: {}", id);
            recetaServicio.actualizarReceta(id, nombre, descripcion, ingredienteIds, cantidades);
            log.info("✅ Receta actualizada exitosamente ID: {}", id);
        } else {
            // Para crear nueva, puedes mantener tu lógica actual o crear método similar en
            // servicio
            log.debug("Creando nueva receta: {}", nombre);
            Receta receta = new Receta();
            receta.setNombre(nombre);
            receta.setDescripcion(descripcion);

            List<RecetaDetalle> detalles = new ArrayList<>();
            if (ingredienteIds != null && cantidades != null) {
                for (int i = 0; i < ingredienteIds.size(); i++) {
                    if (ingredienteIds.get(i) != null && cantidades.get(i) > 0) {
                        RecetaDetalle detalle = new RecetaDetalle();
                        detalle.setIngrediente(ingredienteServicio.buscarIngrediente(ingredienteIds.get(i)));
                        detalle.setCantidadIngrediente(cantidades.get(i));
                        detalle.setReceta(receta);
                        detalles.add(detalle);
                        
                        log.trace("Agregando ingrediente ID: {}, Cantidad: {}", 
                                 ingredienteIds.get(i), cantidades.get(i));
                    }
                }
            }
            receta.setIngredientes(detalles);
            Receta recetaCreada = recetaServicio.crearReceta(receta);
            log.info("✅ Receta creada exitosamente: {} (ID: {}) con {} ingredientes", 
                    recetaCreada.getNombre(), recetaCreada.getId(), detalles.size());
        }

        return "redirect:/recetas";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable long id, Model model) {
        log.info("✏️ GET /recetas/editar/{} - Editando receta", id);
        Receta receta = recetaServicio.buscarReceta(id);
        model.addAttribute("receta", receta);
        model.addAttribute("ingredientes", ingredienteServicio.listaIngredientes());
        model.addAttribute("titulo", "Editar Receta");
        
        log.debug("Receta encontrada: '{}' con {} ingredientes", 
                 receta.getNombre(), receta.getIngredientes().size());
        return "recetas/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarReceta(@PathVariable int id) {
        log.info("🗑️ GET /recetas/eliminar/{} - Eliminando receta", id);
        recetaServicio.eliminarReceta(id);
        log.info("✅ Receta eliminada ID: {}", id);
        return "redirect:/recetas";
    }
}