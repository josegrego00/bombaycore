package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Factura;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.RecetaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ClienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.FacturaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProductoRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class FacturaServicio {

    private static final Logger log = LoggerFactory.getLogger(FacturaServicio.class);

    private final FacturaRepositorio facturaRepo;
    private final ClienteRepositorio clienteRepo;
    private final ProductoServicio productoServicio;
    private final ProductoRepositorio productoRepo;
    private final IngredienteServicio ingredienteServicio;
    private final CierreInventarioDiarioRepositorio cierreRepo;

    public Factura crearFactura(Factura factura) {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        long empresaId = TenantContext.getCurrentTenant();

        log.info("Creando nueva factura para empresa ID: {}, Fecha: {}", empresaId, hoy);

        // Verificar si día anterior está cerrado
        boolean diaAnteriorCerrado = cierreRepo.existeCierrePorFechaYEstadoYEmpresaId(ayer, "COMPLETADO", empresaId);

        if (!diaAnteriorCerrado) {
            log.error("Intento de crear factura sin cierre del día anterior. Empresa ID: {}, Fecha ayer: {}",
                    empresaId, ayer);
            throw new IllegalStateException(
                    "No se pueden crear facturas. " +
                            "Debes cerrar el día " + ayer + " primero. " +
                            "Ve a: /cierres/obligatorio");
        }

        log.debug("Validación de cierre diario exitosa para empresa ID: {}", empresaId);

        // Validar cliente si existe
        if (factura.getCliente() != null && factura.getCliente().getId() > 0) {
            log.debug("Validando cliente ID: {} para empresa ID: {}", factura.getCliente().getId(), empresaId);
            Cliente cliente = clienteRepo.findByIdAndEmpresaId(factura.getCliente().getId(), empresaId)
                    .orElseThrow(() -> {
                        log.error("Cliente no encontrado ID: {} para empresa ID: {}",
                                factura.getCliente().getId(), empresaId);
                        return new RuntimeException("Cliente no encontrado");
                    });
            factura.setCliente(cliente);
            log.debug("Cliente asignado: {} (ID: {})", cliente.getNombre(), cliente.getId());
        }

        // Asignar número de factura si no tiene
        if (factura.getNumeroFactura() == null || factura.getNumeroFactura().isEmpty()) {
            String numeroFactura = generarNumeroFactura(empresaId);
            factura.setNumeroFactura(numeroFactura);
            log.debug("Número de factura generado: {}", numeroFactura);
        } else {
            log.debug("Usando número de factura proporcionado: {}", factura.getNumeroFactura());
        }

        // Validar stock y preparar detalles
        double totalFactura = 0;
        int totalDetalles = factura.getFacturaDetalle().size();
        log.debug("Procesando {} detalles de factura", totalDetalles);

        for (FacturaDetalle detalle : factura.getFacturaDetalle()) {
            // Obtener producto actualizado
            Producto producto = productoServicio.buscarProducto(detalle.getProducto().getId());
            log.trace("Procesando producto: '{}' (ID: {}), Cantidad: {}",
                    producto.getNombre(), producto.getId(), detalle.getCantidad());

            // ✅ REEMPLAZA CON ESTA VALIDACIÓN SIMPLE (sin descontar):
            if (producto.getStock() < detalle.getCantidad()) {
                log.error("Stock insuficiente para producto: '{}'. Stock actual: {}, Cantidad requerida: {}",
                        producto.getNombre(), producto.getStock(), detalle.getCantidad());
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
            }

            log.debug("Stock validado exitosamente para producto: '{}'", producto.getNombre());

            // Asignar precio unitario desde producto
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            // No asignes subtotal aquí - lo hará @PrePersist en FacturaDetalle
            detalle.setFactura(factura);

            // Calcula subtotal para el total temporal
            double subtotalDetalle = detalle.getCantidad() * detalle.getPrecioUnitario();
            totalFactura += subtotalDetalle;

            log.trace("Detalle preparado - Producto: {}, Cantidad: {}, Precio unitario: {}, Subtotal calculado: {}",
                    producto.getNombre(), detalle.getCantidad(), detalle.getPrecioUnitario(), subtotalDetalle);
        }

        // Calcular totales
        double base = totalFactura / 1.19;
        double iva = base * 0.19;

        factura.setSubtotal(Math.round(base * 100.0) / 100.0);
        factura.setIva(Math.round(iva * 100.0) / 100.0);
        factura.setTotal(Math.round(totalFactura * 100.0) / 100.0);

        factura.setEstado("PAGADA");
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        factura.setEmpresa(empresa);

        // Guardar la factura (se ejecutará @PrePersist de FacturaDetalle)
        Factura facturaCreada = facturaRepo.save(factura);

        log.info("Factura guardada: {} (ID: {})",
                facturaCreada.getNumeroFactura(), facturaCreada.getId());

        // ✅ AHORA SÍ DESCONTAMOS STOCK - SOLO UNA VEZ
        for (FacturaDetalle detalle : facturaCreada.getFacturaDetalle()) {
            log.debug("Descontando stock final para producto ID: {}, Cantidad: {}",
                    detalle.getProducto().getId(), detalle.getCantidad());

            if (!productoServicio.descontarStock(detalle.getProducto().getId(), detalle.getCantidad())) {
                // Esto no debería pasar porque ya validamos, pero por seguridad
                log.error("ERROR CRÍTICO: Stock insuficiente después de guardar factura");
                // Puedes revertir la factura o lanzar excepción
                throw new RuntimeException("Error en descuento de stock para: " + detalle.getProducto().getNombre());
            }
        }

        log.info("Factura creada exitosamente: {} (ID: {}) para empresa ID: {}. Total: {}, Detalles: {}",
                facturaCreada.getNumeroFactura(), facturaCreada.getId(), empresaId,
                facturaCreada.getTotal(), totalDetalles);

        return facturaCreada;
    }

    public List<Factura> listarFacturas() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando facturas para empresa ID: {}", empresaId);

        List<Factura> facturas = facturaRepo.findByEmpresaIdWithDetalle(empresaId);
        log.debug("Encontradas {} facturas para empresa ID: {}", facturas.size(), empresaId);

        return facturas;
    }

    public Factura buscarFactura(Long id) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando factura ID: {} para empresa ID: {}", id, empresaId);

        return facturaRepo.findByIdWithDetalle(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Factura no encontrada ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Factura no encontrada");
                });
    }

    public void anularFactura(long id) {
        long empresa_id = TenantContext.getCurrentTenant();
        log.info("Anulando factura ID: {} para empresa ID: {}", id, empresa_id);

        Factura factura = buscarFactura(id);
        if (!factura.getEstado().equals("PAGADA")) {
            log.warn("Intento de anular factura no PAGADA. Factura ID: {}, Estado actual: {}",
                    id, factura.getEstado());
            throw new RuntimeException("Solo se pueden anular facturas pendientes");
        }

        log.debug("Reintegrando stock para {} detalles de factura", factura.getFacturaDetalle().size());
        // Reintegrar stock
        for (FacturaDetalle detalle : factura.getFacturaDetalle()) {
            // Obtener producto fresco de la BD
            Producto producto = productoServicio.buscarProducto(detalle.getProducto().getId());
            log.trace("Reintegrando producto: '{}' (ID: {}), Cantidad: {}",
                    producto.getNombre(), producto.getId(), detalle.getCantidad());

            if (producto.isTieneReceta()) {
                // Reintegrar ingredientes (lógica inversa)
                reintegrarStockReceta(producto.getReceta(), detalle.getCantidad());
                log.debug("Stock de receta reintegrado para producto: '{}'", producto.getNombre());
            } else {
                double stockAnterior = producto.getStock();
                producto.setStock(stockAnterior + detalle.getCantidad());
                productoRepo.save(producto);
                log.debug("Stock directo reintegrado para producto '{}': Anterior: {}, Nuevo: {}, Cantidad: {}",
                        producto.getNombre(), stockAnterior, producto.getStock(), detalle.getCantidad());
            }
        }

        factura.setEstado("ANULADA");
        factura.setSubtotal(0);
        factura.setIva(0.0);
        factura.setTotal(0.0);

        Factura facturaAnulada = facturaRepo.save(factura);
        log.info("Factura anulada exitosamente: {} (ID: {}) para empresa ID: {}",
                facturaAnulada.getNumeroFactura(), id, empresa_id);
    }

    private void calcularTotales(Factura factura) {
        log.debug("Calculando totales para factura");

        double total = factura.getFacturaDetalle().stream()
                .mapToDouble(FacturaDetalle::getSubtotal)
                .sum();

        // Calcular base (subtotal sin IVA)
        double base = total / 1.19;
        double iva = base * 0.19;

        // Redondear a 2 decimales
        base = Math.round(base * 100.0) / 100.0;
        iva = Math.round(iva * 100.0) / 100.0;

        factura.setSubtotal(base);
        factura.setIva(iva);
        factura.setTotal(total); // El total ya incluye IVA

        log.trace("Totales calculados - Subtotal: {}, IVA: {}, Total: {}", base, iva, total);
    }

    private String generarNumeroFactura(long empresaId) {
        Long consecutivo = facturaRepo.countByEmpresaId(empresaId) + 1;
        String numeroFactura = String.format("FAC-%03d-%d", consecutivo, LocalDate.now().getYear());
        log.debug("Consecutivo generado: {} para empresa ID: {}", consecutivo, empresaId);
        return numeroFactura;
    }

    private void reintegrarStockReceta(Receta receta, int cantidad) {
        log.debug("Reintegrando stock de receta '{}' para {} unidades", receta.getNombre(), cantidad);

        for (RecetaDetalle detalle : receta.getIngredientes()) {
            double cantidadReintegrar = detalle.getCantidadIngrediente() * cantidad;
            log.trace("Reintegrando ingrediente: '{}', Cantidad: {}",
                    detalle.getIngrediente().getNombre(), cantidadReintegrar);

            // Usar método que incremente stock
            ingredienteServicio.aumentarStock(
                    detalle.getIngrediente().getId(),
                    cantidadReintegrar);
        }

        log.debug("Stock de receta reintegrado para {} ingredientes", receta.getIngredientes().size());
    }

    public Map<String, Object> obtenerResumenVentasHoy() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Obteniendo resumen de ventas hoy para empresa ID: {}", empresaId);

        // Usar métodos con filtro empresa
        List<Factura> facturasHoy = facturaRepo.findFacturasHoy(empresaId);
        Double totalHoy = facturaRepo.getTotalVentasHoy(empresaId);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("cantidadFacturas", facturasHoy.size());
        resumen.put("totalVentas", totalHoy != null ? totalHoy : 0.0);
        resumen.put("facturas", facturasHoy);

        log.info("Resumen de ventas hoy - Empresa ID: {}, Facturas: {}, Total: {}",
                empresaId, facturasHoy.size(), resumen.get("totalVentas"));

        return resumen;
    }

    
    public Page<Factura> obtenerFacturasPaginadas(
            int pagina,
            int tamanio,
            String estado,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Obteniendo facturas paginadas para empresa ID: {}", empresaId);

        Pageable pageable = PageRequest.of(
                pagina,
                tamanio,
                Sort.by("fecha").descending());

        return facturaRepo.buscarConFiltros(
                empresaId, estado, fechaInicio, fechaFin, pageable);
    }

}