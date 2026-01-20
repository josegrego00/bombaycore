package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ProductoServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.RecetaServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/productos")
@AllArgsConstructor
public class ProductoControlador {

    private static final Logger log = LoggerFactory.getLogger(ProductoControlador.class);
    
    private final ProductoServicio productoServicio;
    private final RecetaServicio recetaServicio;

    @GetMapping
    public String listarProductos(Model modelo) {
        log.info("📋 GET /productos - Listando productos");
        
        List<Producto> productos = productoServicio.listarProductos();
        log.debug("Encontrados {} productos", productos.size());
        
        // Mapa: productoId -> stockPosible
        Map<Long, Integer> stockPosibleMap = new HashMap<>();
        Map<Integer, String> ingredienteLimitanteMap = new HashMap<>();

        for (Producto producto : productos) {
            if (producto.isTieneReceta()) {
                int stockPosible = productoServicio.calcularStockPosible(producto.getId());
                stockPosibleMap.put(producto.getId(), stockPosible);
                log.trace("Producto con receta: {} (ID: {}), Stock posible: {}", 
                         producto.getNombre(), producto.getId(), stockPosible);
            }
        }

        modelo.addAttribute("productos", productos);
        modelo.addAttribute("stockPosibleMap", stockPosibleMap);
        modelo.addAttribute("ingredienteLimitanteMap", ingredienteLimitanteMap);

        log.debug("Stock posible calculado para {} productos con receta", stockPosibleMap.size());
        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model modelo) {
        log.info("📝 GET /productos/nuevo - Mostrando formulario nuevo producto");
        modelo.addAttribute("producto", new Producto());
        modelo.addAttribute("recetas", recetaServicio.listaRecetas());
        return "productos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id, Model modelo) {
        log.info("✏️ GET /productos/editar/{} - Editando producto", id);
        Producto producto = productoServicio.buscarProducto(id);
        modelo.addAttribute("producto", producto);
        modelo.addAttribute("recetas", recetaServicio.listaRecetas());
        
        log.debug("Producto encontrado: '{}', Tiene receta: {}, Receta ID: {}", 
                 producto.getNombre(), producto.isTieneReceta(), 
                 producto.getReceta() != null ? producto.getReceta().getId() : "null");
        return "productos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarProducto(
            @RequestParam(required = false) Integer id,
            @RequestParam String nombre,
            @RequestParam(required = false) Boolean tieneReceta,
            @RequestParam(required = false) Integer recetaId, // ID de receta, no objeto
            @RequestParam double precioVenta,
            @RequestParam double stock,
            @RequestParam String unidadMedidaVenta) {

        log.info("💾 POST /productos/guardar - Guardando producto. ID: {}, Nombre: {}", id, nombre);
        log.debug("Parámetros - Tiene receta: {}, Receta ID: {}, Precio: {}, Stock: {}", 
                 tieneReceta, recetaId, precioVenta, stock);

        boolean esConReceta = tieneReceta != null ? tieneReceta : false;

        Producto producto;
        if (id != null && id > 0) {
            log.debug("Actualizando producto existente ID: {}", id);
            producto = productoServicio.buscarProducto(id);
            producto.setNombre(nombre);
            producto.setTieneReceta(esConReceta);
            producto.setPrecioVenta(precioVenta);
            producto.setStock(stock);
            producto.setUnidadMedidaVenta(unidadMedidaVenta);
        } else {
            log.debug("Creando nuevo producto");
            producto = Producto.builder()
                    .nombre(nombre)
                    .tieneReceta(esConReceta)
                    .precioVenta(precioVenta)
                    .stock(stock)
                    .unidadMedidaVenta(unidadMedidaVenta)
                    .activo(true)
                    .build();
        }

        // Manejar receta si aplica
        if (esConReceta && recetaId != null) {
            log.debug("Asignando receta ID: {} al producto", recetaId);
            Receta receta = recetaServicio.buscarReceta(recetaId);
            producto.setReceta(receta);
        } else {
            log.debug("Producto sin receta o receta no especificada");
            producto.setReceta(null);
        }

        if (id != null && id > 0) {
            productoServicio.actualizarProducto(id, producto);
            log.info("✅ Producto actualizado exitosamente: {} (ID: {})", producto.getNombre(), id);
        } else {
            Producto productoCreado = productoServicio.crearProducto(producto);
            log.info("✅ Producto creado exitosamente: {} (ID: {})", 
                    productoCreado.getNombre(), productoCreado.getId());
        }

        return "redirect:/productos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable int id) {
        log.info("🗑️ GET /productos/eliminar/{} - Eliminando producto", id);
        productoServicio.eliminarProducto(id);
        log.info("✅ Producto eliminado ID: {}", id);
        return "redirect:/productos";
    }
}