package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

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

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaProveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Proveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.FacturaProveedorServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.IngredienteServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ProveedorServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/compras")
@AllArgsConstructor
public class FacturaProveedorControlador {

    private static final Logger log = LoggerFactory.getLogger(FacturaProveedorControlador.class);
    
    private final FacturaProveedorServicio compraServicio;
    private final ProveedorServicio proveedorServicio;
    private final IngredienteServicio ingredienteServicio;

    @GetMapping
    public String listarCompras(Model model) {
        log.info("📋 GET /compras - Listando compras a proveedores");
        
        List<FacturaProveedor> compras = compraServicio.listarFacturasProveedor();
        log.debug("Encontradas {} compras", compras.size());

        // Calcular estadísticas
        long pagadas = compras.stream().filter(c -> "PAGADA".equals(c.getEstado())).count();
        long anuladas = compras.stream().filter(c -> "ANULADA".equals(c.getEstado())).count();
        double totalValor = compras.stream().mapToDouble(FacturaProveedor::getTotal).sum();

        model.addAttribute("compras", compras);
        model.addAttribute("totalPagadas", pagadas);
        model.addAttribute("totalAnuladas", anuladas);
        model.addAttribute("totalValor", totalValor);

        log.debug("Estadísticas - Pagadas: {}, Anuladas: {}, Total valor: {}", 
                 pagadas, anuladas, totalValor);
        return "compras/lista";
    }

    @GetMapping("/nueva")
    public String nuevaCompra(Model model) {
        log.info("📝 GET /compras/nueva - Mostrando formulario nueva compra");
        
        FacturaProveedor compra = new FacturaProveedor();

        // Obtener proveedores activos
        List<Proveedor> proveedores = proveedorServicio.listarProveedores();
        proveedores.removeIf(p -> !p.getActivo());

        // Obtener ingredientes
        List<Ingrediente> ingredientes = ingredienteServicio.listaIngredientes();

        model.addAttribute("compra", compra);
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("ingredientes", ingredientes);

        log.debug("Formulario cargado - Proveedores activos: {}, Ingredientes: {}", 
                 proveedores.size(), ingredientes.size());
        return "compras/formulario";
    }

    @PostMapping("/guardar")
    public String guardarCompra(@ModelAttribute FacturaProveedor compra) {
        log.info("💾 POST /compras/guardar - Guardando compra a proveedor");
        log.debug("Datos compra - Proveedor ID: {}, Número factura: {}", 
                 compra.getProveedor() != null ? compra.getProveedor().getId() : "null",
                 compra.getNumeroFactura());
        
        FacturaProveedor compraCreada = compraServicio.crearFacturaProveedor(compra);
        log.info("✅ Compra a proveedor guardada exitosamente: {} (ID: {})", 
                compraCreada.getNumeroFactura(), compraCreada.getId());
        return "redirect:/compras";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        log.info("👁️ GET /compras/detalle/{} - Viendo detalle de compra", id);
        
        FacturaProveedor compra = compraServicio.buscarFacturaProveedor(id);
        model.addAttribute("compra", compra);
        
        log.debug("Compra encontrada: {}, Estado: {}, Total: {}", 
                 compra.getNumeroFactura(), compra.getEstado(), compra.getTotal());
        return "compras/detalle";
    }

    @GetMapping("/anular/{id}")
    public String anularCompra(@PathVariable Long id) {
        log.info("🔄 GET /compras/anular/{} - Anulando compra", id);
        
        try {
            compraServicio.anularFacturaProveedor(id);
            log.info("✅ Compra anulada exitosamente ID: {}", id);
            return "redirect:/compras?anulada=true";
        } catch (RuntimeException e) {
            log.error("❌ Error al anular compra ID: {} - {}", id, e.getMessage(), e);
            return "redirect:/compras/detalle/" + id + "?error=" + e.getMessage();
        }
    }
}