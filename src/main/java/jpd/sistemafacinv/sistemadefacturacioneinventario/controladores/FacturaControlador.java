package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Factura;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.ProductoPOSDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ClienteServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.FacturaServicio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ProductoServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/facturas")
@AllArgsConstructor
public class FacturaControlador {

    private static final Logger log = LoggerFactory.getLogger(FacturaControlador.class);

    private final FacturaServicio facturaServicio;
    private final ClienteServicio clienteServicio;
    private final ProductoServicio productoServicio;
    private final CierreInventarioDiarioRepositorio cierreRepo;

    // ========== RUTAS ADMIN (Solo visualización) ==========
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public String listarFacturas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model modelo) {

        log.info("Listando facturas - Página: {}, Tamaño: {}", pagina, tamanio);

        Page<Factura> paginaFacturas = facturaServicio.obtenerFacturasPaginadas(
                pagina, tamanio, estado, fechaInicio, fechaFin);

        modelo.addAttribute("facturas", paginaFacturas.getContent());
        modelo.addAttribute("paginaActual", pagina);
        modelo.addAttribute("totalPaginas", paginaFacturas.getTotalPages());
        modelo.addAttribute("totalElementos", paginaFacturas.getTotalElements());
        modelo.addAttribute("tamanio", tamanio);

        return "facturas/lista";
    }

    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public String verDetalleFactura(@PathVariable Long id, Model modelo) {
        log.info("👁️ GET /facturas/detalle/{} - Viendo detalle de factura", id);
        Factura factura = facturaServicio.buscarFactura(id);
        modelo.addAttribute("factura", factura);
        return "facturas/detalle";
    }

    @GetMapping("/anular/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public String anularFactura(@PathVariable int id) {
        log.info("🔄 GET /facturas/anular/{} - Anulando factura", id);
        facturaServicio.anularFactura(id);
        log.info("✅ Factura anulada ID: {}", id);
        return "redirect:/facturas";
    }

    @GetMapping("/imprimir/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public String imprimirFactura(@PathVariable Long id, Model modelo) {
        log.info("🖨️ GET /facturas/imprimir/{} - Imprimiendo factura", id);
        Factura factura = facturaServicio.buscarFactura(id);
        modelo.addAttribute("factura", factura);
        return "facturas/imprimir";
    }

    // ========== RUTAS USUARIO (Punto de Venta) ==========
    @GetMapping("/facturar")
    @PreAuthorize("hasRole('CAJERO')")
    public String mostrarPuntoVenta(Model modelo) {
        log.info("💰 GET /facturas/facturar - Mostrando punto de venta (CAJERO)");

        LocalDate hoy = LocalDate.now();
        CierreInventarioDiario cierreHoy = cierreRepo.findCierreCompletadoByFecha(hoy);

        if (cierreHoy != null && cierreHoy.getEstado().equals("COMPLETADO")) {
            log.warn("⛔ Día ya cerrado. No se pueden crear nuevas facturas. Fecha: {}", hoy);
            // Redirigir con mensaje de error
            return "redirect:/facturas?error=El día ya está cerrado. No se pueden crear nuevas facturas.";
        }
        List<Producto> productos = productoServicio.listarProductos();

        List<ProductoPOSDTO> productosPOS = productos.stream().map(p -> {

            ProductoPOSDTO dto = new ProductoPOSDTO();
            dto.setId(p.getId());
            dto.setNombre(p.getNombre());
            dto.setPrecio(p.getPrecioVenta());
            dto.setTieneReceta(p.isTieneReceta());

            double stock;

            if (p.isTieneReceta()) {
                stock = productoServicio.calcularStockPosible(p.getId());
            } else {
                stock = p.getStock();
            }

            dto.setStockPosible(stock);
            return dto;

        }).toList();

        modelo.addAttribute("clientes", clienteServicio.listarClientes());
        modelo.addAttribute("productos", productosPOS);
        modelo.addAttribute("fechaHoy", LocalDate.now());

        log.debug("Punto de venta cargado - Clientes: {}, Productos: {}",
                clienteServicio.listarClientes().size(), productoServicio.listarProductos().size());
        return "facturas/punto-venta";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('CAJERO')")
    public String guardarFactura(
            @RequestParam(required = false) Long clienteId,
            @RequestParam String formaPago,
            @RequestParam(value = "productoIds", required = false) List<Integer> productoIds,
            @RequestParam(value = "cantidades", required = false) List<Integer> cantidades) {

        log.info("💾 POST /facturas/guardar - Guardando nueva factura");
        log.debug("Parámetros - Cliente ID: {}, Forma pago: {}, Productos: {}",
                clienteId, formaPago, productoIds != null ? productoIds.size() : 0);

        // Construir factura
        Factura factura = new Factura();
        factura.setFormaPago(formaPago);

        // Asignar cliente si existe
        if (clienteId != null && clienteId > 0) {
            log.debug("Asignando cliente ID: {}", clienteId);
            Cliente cliente = clienteServicio.buscarCliente(clienteId);
            factura.setCliente(cliente);
        } else {
            log.debug("Usando cliente por defecto ID: 1");
            Cliente cliente = clienteServicio.buscarCliente(1L);
            factura.setCliente(cliente);
        }

        // Construir detalles
        List<FacturaDetalle> detalles = new ArrayList<>();
        if (productoIds != null && cantidades != null) {
            for (int i = 0; i < productoIds.size(); i++) {
                if (productoIds.get(i) != null && cantidades.get(i) > 0) {
                    Producto producto = productoServicio.buscarProducto(productoIds.get(i));

                    FacturaDetalle detalle = FacturaDetalle.builder()
                            .producto(producto)
                            .cantidad(cantidades.get(i))
                            .precioUnitario(producto.getPrecioVenta())
                            .factura(factura)
                            .build();

                    detalles.add(detalle);
                    log.trace("Producto agregado a factura: {} (ID: {}), Cantidad: {}",
                            producto.getNombre(), productoIds.get(i), cantidades.get(i));
                }
            }
        }

        factura.setFacturaDetalle(detalles);
        log.debug("Factura construida con {} detalles", detalles.size());

        try { // Guardar factura
            Factura facturaCreada = facturaServicio.crearFactura(factura);
            log.info("✅ Factura creada exitosamente: {} (ID: {})",
                    facturaCreada.getNumeroFactura(), facturaCreada.getId());

            // Redirigir al detalle de la factura creada
            return "redirect:/facturas/detalle/" + facturaCreada.getId();

        } catch (IllegalStateException e) {
            log.error("❌ Error creando factura - Día anterior no cerrado: {}", e.getMessage());
            // Redirigir con mensaje de error
            return "redirect:/cierres/obligatorio";
        } catch (Exception e) {
            log.error("❌ Error inesperado creando factura: {}", e.getMessage(), e);
            return "redirect:/facturas/facturar?error=" + e.getMessage();
        }
    }

    @GetMapping("/ventas-hoy")
    @PreAuthorize("hasRole('CAJERO')")
    public String ventasHoy(Model model) {
        log.info("📈 GET /facturas/ventas-hoy - Mostrando ventas del día");
        Map<String, Object> resumen = facturaServicio.obtenerResumenVentasHoy();
        model.addAllAttributes(resumen);

        log.debug("Resumen ventas hoy - Facturas: {}, Total: {}",
                resumen.get("cantidadFacturas"), resumen.get("totalVentas"));
        return "facturas/ventas-hoy";
    }

}