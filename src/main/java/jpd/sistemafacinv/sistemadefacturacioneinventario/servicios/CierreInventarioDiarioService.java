package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DetalleCierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Factura;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.DetalleCierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.FacturaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProductoRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CierreInventarioDiarioService {

    private static final Logger log = LoggerFactory.getLogger(CierreInventarioDiarioService.class);

    private final CierreInventarioDiarioRepositorio cierreRepository;
    private final DetalleCierreInventarioDiarioRepositorio detalleCierreRepository;
    private final IngredienteRepositorio ingredienteRepository;
    private final ProductoRepositorio productoRepository;
    private final FacturaRepositorio facturaRepository;
    private final EmpresaRepositorio empresaRepositorio;

    // 1. INICIAR NUEVO CIERRE
    // esto es un String miestras no haya un sistema de usuarios
    public CierreInventarioDiario iniciarNuevoCierre(Usuario usuario) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.info("Iniciando nuevo cierre diario para empresa ID: {}, Usuario: {}", empresaId,
                usuario.getNombreUsuario());

        Empresa empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada ID: {}", empresaId);
                    return new RuntimeException("Empresa no encontrada: " + empresaId);
                });

        // Verificar si ya hay cierre hoy
        LocalDate hoy = LocalDate.now();
        Optional<CierreInventarioDiario> cierreExistente = cierreRepository.findByFechaAndEmpresaId(hoy,
                empresaId);

        if (cierreExistente.isPresent()) {
            log.warn("Ya existe un cierre para hoy: {} para empresa ID: {}", hoy, empresaId);
            throw new RuntimeException("Ya existe un cierre para hoy");
        }
        log.debug("No hay cierre existente para hoy: {}", hoy);

        // Crear cabecera
        CierreInventarioDiario cierre = CierreInventarioDiario.builder()
                .fecha(hoy)
                .usuario(usuario)
                .estado("EN_PROCESO")
                .observaciones("Cierre diario " + hoy)
                .empresa(empresa)
                .build();

        cierre = cierreRepository.save(cierre);
        log.info("Cierre creado ID: {} para empresa ID: {}, Fecha: {}", cierre.getId(), empresaId, hoy);

        // Precargar detalles con stock teórico
        precargarDetallesCierre(cierre);

        return cierre;
    }

    // 2. PRECARGAR DETALLES (ingredientes + productos sin receta)
    private void precargarDetallesCierre(CierreInventarioDiario cierre) {
        log.debug("Precargando detalles para cierre ID: {}", cierre.getId());

        List<DetalleCierreInventarioDiario> detalles = new ArrayList<>();
        // OBTENER EMPRESA DEL CIERRE
        Empresa empresa = cierre.getEmpresa();

        // Ingredientes (materia prima)
        List<Ingrediente> ingredientes = ingredienteRepository.findByEmpresaId(empresa.getId());
        log.debug("Encontrados {} ingredientes para empresa ID: {}", ingredientes.size(), empresa.getId());

        for (Ingrediente ing : ingredientes) {
            DetalleCierreInventarioDiario detalle = DetalleCierreInventarioDiario.builder()
                    .cierre(cierre)
                    .ingrediente(ing)
                    .stockTeorico(ing.getStockActual())
                    .costoUnitario(ing.getPrecio())
                    .stockReal(0) // Por llenar
                    .stockMerma(0) // por llenar
                    .stockDesperdicio(0) // por llenar
                    .diferencia(0) // el sistema lo calcula
                    .valorDiferencia(0) // el sistema lo calcula
                    .build();
            detalles.add(detalle);
            log.trace("Detalle precargado - Ingrediente: {} (ID: {}), Stock teórico: {}",
                    ing.getNombre(), ing.getId(), ing.getStockActual());
        }

        // Productos sin receta (comprados para revender)
        List<Producto> productosSinReceta = productoRepository.findByEmpresaIdAndTieneRecetaFalse(empresa.getId());
        log.debug("Encontrados {} productos sin receta para empresa ID: {}", productosSinReceta.size(),
                empresa.getId());

        for (Producto prod : productosSinReceta) {
            DetalleCierreInventarioDiario detalle = DetalleCierreInventarioDiario.builder()
                    .cierre(cierre)
                    .producto(prod)
                    .stockTeorico(prod.getStock())
                    .costoUnitario(prod.getPrecioVenta())
                    .stockReal(0)
                    .stockMerma(0)
                    .stockDesperdicio(0)
                    .diferencia(0)
                    .valorDiferencia(0)
                    .build();
            detalles.add(detalle);
            log.trace("Detalle precargado - Producto: {} (ID: {}), Stock teórico: {}",
                    prod.getNombre(), prod.getId(), prod.getStock());
        }

        detalleCierreRepository.saveAll(detalles);
        log.info("Detalles precargados: {} totales para cierre ID: {}", detalles.size(), cierre.getId());
    }

    // 3. ACTUALIZAR DETALLE (stock real, merma, desperdicio)
    public void actualizarDetalle(Long detalleId, double stockReal,
            double merma, double desperdicio) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Actualizando detalle ID: {} para empresa ID: {}", detalleId, empresaId);

        DetalleCierreInventarioDiario detalle = detalleCierreRepository.findByIdAndCierreEmpresaId(detalleId, empresaId)
                .orElseThrow(() -> {
                    log.error("Detalle no encontrado ID: {} para empresa ID: {}", detalleId, empresaId);
                    return new RuntimeException("Detalle no encontrado");
                });

        log.trace("Detalle encontrado - Stock teórico: {}, Stock real anterior: {}",
                detalle.getStockTeorico(), detalle.getStockReal());

        detalle.setStockReal(stockReal);
        detalle.setStockMerma(merma);
        detalle.setStockDesperdicio(desperdicio);

        // Calcular diferencia
        double diferencia = ((stockReal + detalle.getStockMerma() + detalle.getStockDesperdicio())
                - detalle.getStockTeorico());
        detalle.setDiferencia(diferencia);

        // Calcular valor diferencia
        double costo = 0;

        if (detalle.getIngrediente() != null) {
            costo = detalle.getIngrediente().getPrecio();
            log.trace("Es ingrediente: {} (ID: {}), Costo: {}",
                    detalle.getIngrediente().getNombre(), detalle.getIngrediente().getId(), costo);
        } else if (detalle.getProducto() != null) {
            costo = detalle.getProducto().getPrecioVenta();
            log.trace("Es producto: {} (ID: {}), Costo: {}",
                    detalle.getProducto().getNombre(), detalle.getProducto().getId(), costo);
        }
        // Actualizar costo unitario por si cambió
        detalle.setCostoUnitario(costo);

        double valor = diferencia * costo;
        detalle.setValorDiferencia(valor);

        detalleCierreRepository.save(detalle);
        log.info("Detalle actualizado ID: {}, Stock real: {}, Diferencia: {}, Valor diferencia: {}",
                detalleId, stockReal, diferencia, valor);
    }

    // 4. COMPLETAR CIERRE (calcular totales y actualizar inventario si se desea)
    public void completarCierre(Long cierreId) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.info("Completando cierre ID: {} para empresa ID: {}", cierreId, empresaId);

        CierreInventarioDiario cierre = cierreRepository.findByIdAndEmpresaId(cierreId, empresaId)
                .orElseThrow(() -> {
                    log.error("Cierre no encontrado ID: {} para empresa ID: {}", cierreId, empresaId);
                    return new RuntimeException("Cierre no encontrado");
                });

        List<DetalleCierreInventarioDiario> detalles = detalleCierreRepository.findByCierre(cierre);

        // Validar que todos los detalles tengan stockReal ingresado
        boolean completo = detalles.stream()
                .allMatch(d -> d.getStockReal() >= 0);

        if (!completo) {
            log.warn("Faltan detalles por completar en cierre ID: {}. Detalles: {}", cierreId, detalles.size());
            throw new RuntimeException("Faltan detalles por completar");
        }

        // Actualizar estado
        cierre.setEstado("PRE-COMPLETADO");
        cierreRepository.save(cierre);

        log.info("Cierre marcado como PRE-COMPLETADO ID: {} para empresa ID: {}", cierreId, empresaId);
    }

    public void completarCierreDefinitivo(Long cierreId) {
        log.info("Completando cierre definitivo ID: {}", cierreId);

        CierreInventarioDiario cierre = buscarCierre(cierreId);

        // Verificar que esté en PRE-COMPLETADO
        if (!"PRE-COMPLETADO".equals(cierre.getEstado())) {
            log.error("Cierre no está en PRE-COMPLETADO. Estado actual: {}, Cierre ID: {}", cierre.getEstado(),
                    cierreId);
            throw new RuntimeException("El cierre debe estar en PRE-COMPLETADO");
        }

        log.debug("Calculando ventas del día para cierre ID: {}", cierreId);
        // Calcular ventas del día
        calcularVentasDelDia(cierre);

        List<DetalleCierreInventarioDiario> detalles = detalleCierreRepository.findByCierre(cierre);
        log.debug("Ajustando inventario para {} detalles", detalles.size());
        ajustarInventarioALoReal(detalles);

        cierre.setEstado("COMPLETADO");
        CierreInventarioDiario cierreCompletado = cierreRepository.save(cierre);

        log.info(
                "Cierre completado definitivamente ID: {} para empresa ID: {}. Total ventas: {}, Cantidad facturas: {}",
                cierreId, cierre.getEmpresa().getId(), cierreCompletado.getTotalVentas(),
                cierreCompletado.getCantidadFacturas());
    }

    // 5. AJUSTAR INVENTARIO (opcional)
    private void ajustarInventarioALoReal(List<DetalleCierreInventarioDiario> detalles) {
        log.debug("Ajustando inventario para {} detalles", detalles.size());

        int ingredientesAjustados = 0;
        int productosAjustados = 0;

        for (DetalleCierreInventarioDiario detalle : detalles) {
            if (detalle.getIngrediente() != null) {
                // Ajustar ingrediente
                Ingrediente ing = detalle.getIngrediente();
                double stockAnterior = ing.getStockActual();
                ing.setStockActual(detalle.getStockReal());
                ingredienteRepository.save(ing);
                ingredientesAjustados++;

                log.trace("Ingrediente ajustado: {} (ID: {}). Anterior: {}, Nuevo: {}",
                        ing.getNombre(), ing.getId(), stockAnterior, detalle.getStockReal());
            } else if (detalle.getProducto() != null) {
                // Ajustar producto sin receta
                Producto prod = detalle.getProducto();
                double stockAnterior = prod.getStock();
                prod.setStock(detalle.getStockReal());
                productoRepository.save(prod);
                productosAjustados++;

                log.trace("Producto ajustado: {} (ID: {}). Anterior: {}, Nuevo: {}",
                        prod.getNombre(), prod.getId(), stockAnterior, detalle.getStockReal());
            }
        }

        log.info("Inventario ajustado: {} ingredientes, {} productos", ingredientesAjustados, productosAjustados);
    }

    // EN CierreInventarioDiarioService.java

    // 6. OBTENER TODOS LOS CIERRES (para lista histórica)
    public List<CierreInventarioDiario> obtenerTodosCierres() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Obteniendo todos los cierres para empresa ID: {}", empresaId);

        List<CierreInventarioDiario> cierres = cierreRepository.findByEmpresaId(empresaId);
        log.debug("Encontrados {} cierres para empresa ID: {}", cierres.size(), empresaId);

        return cierres;
    }

    // 7. OBTENER CIERRE EN PROCESO ACTUAL
    public Optional<CierreInventarioDiario> obtenerCierreEnProceso() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando cierre en proceso para empresa ID: {}", empresaId);

        Optional<CierreInventarioDiario> cierre = cierreRepository.findByEstadoAndEmpresaId("EN_PROCESO", empresaId);

        if (cierre.isPresent()) {
            log.debug("Cierre en proceso encontrado ID: {} para empresa ID: {}", cierre.get().getId(), empresaId);
        } else {
            log.debug("No hay cierre en proceso para empresa ID: {}", empresaId);
        }

        return cierre;
    }

    // 8. OBTENER DETALLES DE UN CIERRE
    public List<DetalleCierreInventarioDiario> obtenerDetallesCierre(Long cierreId) {
        log.debug("Obteniendo detalles para cierre ID: {}", cierreId);

        CierreInventarioDiario cierre = buscarCierre(cierreId);
        List<DetalleCierreInventarioDiario> detalles = detalleCierreRepository.findByCierre(cierre);

        log.debug("Encontrados {} detalles para cierre ID: {}", detalles.size(), cierreId);
        return detalles;
    }

    public CierreInventarioDiario buscarCierre(Long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando cierre ID: {} para empresa ID: {}", id, empresaId);

        return cierreRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Cierre no encontrado ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Cierre no encontrado");
                });
    }

    private void calcularVentasDelDia(CierreInventarioDiario cierre) {
        LocalDate fecha = cierre.getFecha();
        Long empresaId = cierre.getEmpresa().getId();
        log.debug("Calculando ventas del día {} para empresa ID: {}", fecha, empresaId);

        // Buscar facturas del día
        List<Factura> facturasDelDia = facturaRepository.findByFechaAndEstadoAndEmpresaId(fecha, "PAGADA", empresaId);

        double totalVentas = facturasDelDia.stream()
                .mapToDouble(Factura::getTotal)
                .sum();

        cierre.setTotalVentas(totalVentas);
        cierre.setCantidadFacturas(facturasDelDia.size());

        log.debug("Ventas calculadas: {} facturas, Total: {} para fecha: {}",
                facturasDelDia.size(), totalVentas, fecha);
    }
}