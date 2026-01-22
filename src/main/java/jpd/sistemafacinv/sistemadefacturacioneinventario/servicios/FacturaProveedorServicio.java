package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaDetalleProveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaProveedor;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Proveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.FacturaProveedorRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProveedorRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import lombok.AllArgsConstructor;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class FacturaProveedorServicio {

    private static final Logger log = LoggerFactory.getLogger(FacturaProveedorServicio.class);
    
    private final FacturaProveedorRepositorio facturaRepo;
    private final ProveedorRepositorio proveedorRepo;
    private final IngredienteRepositorio ingredienteRepo;
    private final EmpresaRepositorio empresaRepo;

    public FacturaProveedor crearFacturaProveedor(FacturaProveedor factura) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.info("Creando factura de proveedor para empresa ID: {}", empresaId);

        // 1. OBTENER EMPRESA COMPLETA (NO CREAR NUEVA)
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada ID: {}", empresaId);
                    return new RuntimeException("Empresa no encontrada en el sistema");
                });
        factura.setEmpresa(empresa);
        log.debug("Empresa asignada: {} (ID: {})", empresa.getNombre(), empresa.getId());

        // 2. VALIDAR PROVEEDOR
        if (factura.getProveedor() == null || factura.getProveedor().getId() == null) {
            log.error("Proveedor es requerido pero no fue proporcionado");
            throw new RuntimeException("Proveedor es requerido");
        }

        Proveedor proveedor = proveedorRepo
                .findByIdAndEmpresaId(factura.getProveedor().getId(), empresaId)
                .orElseThrow(() -> {
                    log.error("Proveedor no encontrado ID: {} para empresa ID: {}", 
                             factura.getProveedor().getId(), empresaId);
                    return new RuntimeException("Proveedor no encontrado o no pertenece a esta empresa");
                });
        factura.setProveedor(proveedor);
        log.debug("Proveedor asignado: {} (ID: {})", proveedor.getNombre(), proveedor.getId());

        // 3. VALIDAR/GENERAR NÚMERO DE FACTURA
        if (factura.getNumeroFactura() == null || factura.getNumeroFactura().trim().isEmpty()) {
            // Generar nuevo número
            String numeroGenerado = generarNumeroFactura();
            factura.setNumeroFactura(numeroGenerado);
            log.debug("Número de factura generado: {}", numeroGenerado);
        } else {
            // Validar que no exista el número en esta empresa
            String numeroFactura = factura.getNumeroFactura().trim();
            if (facturaRepo.existsByNumeroFacturaAndEmpresaId(numeroFactura, empresaId)) {
                log.warn("Número de factura duplicado: {} para empresa ID: {}", numeroFactura, empresaId);
                throw new RuntimeException("Ya existe una factura con el número: " + factura.getNumeroFactura());
            }
            log.debug("Número de factura proporcionado: {}", numeroFactura);
        }

        // 4. ESTABLECER FECHA SI NO TIENE
        if (factura.getFecha() == null) {
            factura.setFecha(LocalDate.now());
            log.debug("Fecha establecida a hoy: {}", factura.getFecha());
        }

        // 5. PROCESAR DETALLES
        double subtotal = 0;
        int totalDetalles = factura.getDetalles() != null ? factura.getDetalles().size() : 0;
        log.debug("Procesando {} detalles de factura", totalDetalles);
        
        for (FacturaDetalleProveedor detalle : factura.getDetalles()) {
            if (detalle.getIngrediente() == null || detalle.getIngrediente().getId() == null) {
                log.error("Ingrediente es requerido pero no fue proporcionado en un detalle");
                throw new RuntimeException("Ingrediente es requerido en los detalles");
            }

            // Validar ingrediente pertenece a la empresa
            Ingrediente ingrediente = ingredienteRepo.findByIdAndEmpresaId(
                    detalle.getIngrediente().getId(), empresaId)
                    .orElseThrow(() -> {
                        log.error("Ingrediente no encontrado ID: {} para empresa ID: {}", 
                                 detalle.getIngrediente().getId(), empresaId);
                        return new RuntimeException(
                                "Ingrediente no encontrado o no pertenece a esta empresa");
                    });

            // Calcular subtotal
            detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            detalle.setFacturaProveedor(factura); // Establecer relación bidireccional
            subtotal += detalle.getSubtotal();
            
            log.trace("Detalle procesado - Ingrediente: {}, Cantidad: {}, Precio: {}, Subtotal: {}", 
                     ingrediente.getNombre(), detalle.getCantidad(), 
                     detalle.getPrecioUnitario(), detalle.getSubtotal());

            // Aumentar stock
            double stockAnterior = ingrediente.getStockActual();
            ingrediente.setStockActual(stockAnterior + detalle.getCantidad());
            ingredienteRepo.save(ingrediente);
            
            log.debug("Stock actualizado para ingrediente '{}': Anterior: {}, Nuevo: {}, Aumento: {}", 
                     ingrediente.getNombre(), stockAnterior, ingrediente.getStockActual(), detalle.getCantidad());
        }

        // 6. CALCULAR TOTALES
        double iva = subtotal * 0.19;
        double total = subtotal + iva;

        factura.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        factura.setIva(Math.round(iva * 100.0) / 100.0);
        factura.setTotal(Math.round(total * 100.0) / 100.0);
        
        log.debug("Totales calculados - Subtotal: {}, IVA: {}, Total: {}", 
                 factura.getSubtotal(), factura.getIva(), factura.getTotal());

        // 7. ESTABLECER ESTADO POR DEFECTO SI NO TIENE
        if (factura.getEstado() == null || factura.getEstado().trim().isEmpty()) {
            factura.setEstado("PAGADA");
            log.debug("Estado establecido por defecto: PAGADA");
        } else {
            log.debug("Estado proporcionado: {}", factura.getEstado());
        }

        // 8. GUARDAR (esto guardará factura y detalles por cascade)
        FacturaProveedor facturaCreada = facturaRepo.save(factura);
        log.info("Factura de proveedor creada exitosamente: {} (ID: {}) para empresa ID: {}. Total: {}, Detalles: {}", 
                facturaCreada.getNumeroFactura(), facturaCreada.getId(), empresaId, 
                facturaCreada.getTotal(), totalDetalles);
        
        return facturaCreada;
    }

    // Anular factura de proveedor
    public void anularFacturaProveedor(Long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.info("Anulando factura de proveedor ID: {} para empresa ID: {}", id, empresaId);

        FacturaProveedor factura = buscarFacturaProveedor(id);

        if (!"PAGADA".equals(factura.getEstado())) {
            log.warn("Intento de anular factura no PAGADA. Factura ID: {}, Estado actual: {}", 
                    id, factura.getEstado());
            throw new RuntimeException("Solo se pueden anular facturas en estado PAGADA");
        }

        log.debug("Reintegrando stock para {} detalles de factura", factura.getDetalles().size());
        // Reintegrar stock (disminuir lo que se aumentó)
        for (FacturaDetalleProveedor detalle : factura.getDetalles()) {
            Ingrediente ingrediente = ingredienteRepo.findByIdAndEmpresaId(detalle.getIngrediente().getId(), empresaId)
                    .orElseThrow(() -> {
                        log.error("Ingrediente no encontrado ID: {} para empresa ID: {}", 
                                 detalle.getIngrediente().getId(), empresaId);
                        return new RuntimeException("Ingrediente no encontrado");
                    });

            // Validar que haya suficiente stock para disminuir
            if (ingrediente.getStockActual() < detalle.getCantidad()) {
                log.error("Stock insuficiente para anular. Ingrediente: '{}', Stock actual: {}, Cantidad requerida: {}", 
                         ingrediente.getNombre(), ingrediente.getStockActual(), detalle.getCantidad());
                throw new RuntimeException("Stock insuficiente para anular. Ingrediente: " + ingrediente.getNombre());
            }

            // Disminuir stock
            double stockAnterior = ingrediente.getStockActual();
            ingrediente.setStockActual(stockAnterior - detalle.getCantidad());
            ingredienteRepo.save(ingrediente);
            
            log.debug("Stock disminuido para ingrediente '{}': Anterior: {}, Nuevo: {}, Disminución: {}", 
                     ingrediente.getNombre(), stockAnterior, ingrediente.getStockActual(), detalle.getCantidad());
        }

        // Poner totales a 0
        factura.setSubtotal(0.0);
        factura.setIva(0.0);
        factura.setTotal(0.0);
        factura.setEstado("ANULADA");

        FacturaProveedor facturaAnulada = facturaRepo.save(factura);
        log.info("Factura de proveedor anulada exitosamente: {} (ID: {}) para empresa ID: {}", 
                facturaAnulada.getNumeroFactura(), id, empresaId);
    }

    // Métodos auxiliares
    public List<FacturaProveedor> listarFacturasProveedor() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando facturas de proveedor para empresa ID: {}", empresaId);
        
        List<FacturaProveedor> facturas = facturaRepo.findByEmpresaIdOrderByFechaDesc(empresaId);
        log.debug("Encontradas {} facturas de proveedor para empresa ID: {}", facturas.size(), empresaId);
        
        return facturas;
    }

    public FacturaProveedor buscarFacturaProveedor(Long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando factura de proveedor ID: {} para empresa ID: {}", id, empresaId);
        
        return facturaRepo.findByIdWithDetalles(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Factura de proveedor no encontrada ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Factura de proveedor no encontrada");
                });
    }

    private String generarNumeroFactura() {
        Long empresaId = TenantContext.getCurrentTenant();
        Long consecutivo = facturaRepo.countByEmpresaId(empresaId) + 1;
        String numeroFactura = String.format("FAC-PROV-%03d-%d", consecutivo, LocalDate.now().getYear());
        log.debug("Consecutivo generado: {} para empresa ID: {}", consecutivo, empresaId);
        return numeroFactura;
    }
}