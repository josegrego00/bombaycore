package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.RecetaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProductoRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.RecetaRepositorio;
import lombok.*;

@Service
@AllArgsConstructor
@Transactional
public class ProductoServicio {

    private static final Logger log = LoggerFactory.getLogger(ProductoServicio.class);
    
    private final ProductoRepositorio productoRepo;
    private final RecetaRepositorio recetaRepo;
    private final IngredienteRepositorio ingredienteRepo;

    public Producto crearProducto(Producto producto) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Creando producto '{}' para empresa ID: {}", producto.getNombre(), empresaId);
        
        validarProducto(producto);
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        producto.setEmpresa(empresa);

        validarProducto(producto);

        if (producto.isTieneReceta()) {
            log.debug("Producto con receta, validando receta asociada");
            // Validar que tenga receta asociada
            if (producto.getReceta() == null) {
                log.error("Producto con receta pero sin receta asociada: {}", producto.getNombre());
                throw new RuntimeException("Producto con receta debe tener una receta asociada");
            }
            // Asegurar que la receta existe en BD
            Receta receta = recetaRepo.findByIdAndEmpresaId(producto.getReceta().getId(), empresaId)
                    .orElseThrow(() -> {
                        log.error("Receta no encontrada ID: {} para empresa ID: {}", 
                                 producto.getReceta().getId(), empresaId);
                        return new RuntimeException("Receta no encontrada");
                    });
            producto.setReceta(receta);

            // Calcular precios automáticamente desde la receta
            calcularPreciosDesdeReceta(producto);
            log.debug("Precios calculados desde receta para producto '{}'", producto.getNombre());
        } else {
            log.debug("Producto sin receta creado: '{}'", producto.getNombre());
        }
        
        Producto productoCreado = productoRepo.save(producto);
        log.info("Producto creado exitosamente: '{}' (ID: {}) para empresa ID: {}", 
                productoCreado.getNombre(), productoCreado.getId(), empresaId);
        
        return productoCreado;
    }

    public List<Producto> listarProductos() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando productos para empresa ID: {}", empresaId);
        
        List<Producto> productos = productoRepo.findByEmpresaId(empresaId);
        log.debug("Encontrados {} productos para empresa ID: {}", productos.size(), empresaId);
        
        return productos;
    }

    public Producto buscarProducto(long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando producto ID: {} para empresa ID: {}", id, empresaId);
        
        return productoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Producto no encontrado ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Producto no encontrado");
                });
    }

    public Producto actualizarProducto(int id, Producto productoActualizado) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Actualizando producto ID: {} para empresa ID: {}", id, empresaId);
        
        Producto producto = buscarProducto(id);
        log.debug("Producto encontrado para actualizar: '{}' (ID: {})", producto.getNombre(), id);
        
        producto.setNombre(productoActualizado.getNombre());
        producto.setTieneReceta(productoActualizado.isTieneReceta());
        producto.setPrecioVenta(productoActualizado.getPrecioVenta());
        producto.setStock(productoActualizado.getStock());
        producto.setUnidadMedidaVenta(productoActualizado.getUnidadMedidaVenta());
        producto.setActivo(productoActualizado.isActivo());

        // Manejo especial para la receta
        if (producto.isTieneReceta()) {
            log.debug("Producto con receta, validando receta asociada");
            if (productoActualizado.getReceta() == null) {
                log.error("Producto con receta pero sin receta asociada en actualización: {}", producto.getNombre());
                throw new RuntimeException("Producto con receta debe tener una receta asociada");
            }
            // Actualizar receta (traer de BD)
            Receta receta = recetaRepo.findByIdAndEmpresaId(productoActualizado.getReceta().getId(), empresaId)
                    .orElseThrow(() -> {
                        log.error("Receta no encontrada ID: {} para empresa ID: {}", 
                                 productoActualizado.getReceta().getId(), empresaId);
                        return new RuntimeException("Receta no encontrada");
                    });
            producto.setReceta(receta);

            // Recalcular precios desde la nueva receta
            calcularPreciosDesdeReceta(producto);
            log.debug("Precios recalculados desde receta para producto '{}'", producto.getNombre());
        } else {
            log.debug("Producto sin receta, limpiando asociación de receta");
            // Si ahora es sin receta, limpiar la asociación
            producto.setReceta(null);
        }

        Producto productoActualizadoObj = productoRepo.save(producto);
        log.info("Producto actualizado exitosamente: '{}' (ID: {}) para empresa ID: {}", 
                productoActualizadoObj.getNombre(), id, empresaId);
        
        return productoActualizadoObj;
    }

    public void eliminarProducto(long id) {
        log.debug("Eliminando producto ID: {}", id);
        
        Producto producto = buscarProducto(id);
        productoRepo.delete(producto);
        
        log.info("Producto eliminado: '{}' (ID: {})", producto.getNombre(), id);
    }

    // Método para descontar stock cuando se vende
    public boolean descontarStock(long idProducto, int cantidad) {
        log.debug("Descontando stock - Producto ID: {}, Cantidad: {}", idProducto, cantidad);
        
        Producto producto = buscarProducto(idProducto);

        if (producto.isTieneReceta()) {
            log.debug("Descontando ingredientes de receta para producto '{}'", producto.getNombre());
            // Descontar ingredientes de la receta
            return descontarIngredientesReceta(producto.getReceta(), cantidad);
        } else {
            log.debug("Descontando stock directo para producto '{}'", producto.getNombre());
            // Descontar stock directo
            if (producto.getStock() >= cantidad) {
                producto.setStock(producto.getStock() - cantidad);
                productoRepo.save(producto);
                log.info("Stock descontado exitosamente. Producto: '{}', Cantidad: {}, Stock restante: {}", 
                        producto.getNombre(), cantidad, producto.getStock());
                return true;
            }
            log.warn("Stock insuficiente. Producto: '{}', Stock actual: {}, Cantidad requerida: {}", 
                    producto.getNombre(), producto.getStock(), cantidad);
            return false;
        }
    }

    public int calcularStockPosible(long productoId) {
        log.debug("Calculando stock posible para producto ID: {}", productoId);

        Producto producto = buscarProducto(productoId);

        if (!producto.isTieneReceta() || producto.getReceta() == null) {
            log.debug("Producto sin receta o receta nula, stock posible: 0");
            return 0; // Productos sin receta no tienen "stock posible"
        }

        Receta receta = producto.getReceta();
        int maximoPosible = Integer.MAX_VALUE;

        log.debug("Calculando stock posible basado en {} ingredientes", receta.getIngredientes().size());
        for (RecetaDetalle detalle : receta.getIngredientes()) {
            Ingrediente ingrediente = detalle.getIngrediente();
            double cantidadPorUnidad = detalle.getCantidadIngrediente();
            double stockDisponible = ingrediente.getStockActual();

            int maxPorIngrediente = (int) (stockDisponible / cantidadPorUnidad);
            maximoPosible = Math.min(maximoPosible, maxPorIngrediente);
            
            log.trace("Ingrediente: '{}' - Stock: {}, Por unidad: {}, Máx posible: {}", 
                     ingrediente.getNombre(), stockDisponible, cantidadPorUnidad, maxPorIngrediente);
        }

        int resultado = maximoPosible == Integer.MAX_VALUE ? 0 : maximoPosible;
        log.debug("Stock posible calculado para producto '{}': {}", producto.getNombre(), resultado);
        
        return resultado;
    }

    private boolean descontarIngredientesReceta(Receta receta, int cantidad) {
        log.debug("Descontando ingredientes de receta '{}', cantidad: {}", receta.getNombre(), cantidad);
        
        // Primero validar que hay suficiente stock
        for (RecetaDetalle detalle : receta.getIngredientes()) {
            Ingrediente ing = detalle.getIngrediente();
            double cantidadNecesaria = detalle.getCantidadIngrediente() * cantidad;

            if (ing.getStockActual() < cantidadNecesaria) {
                log.warn("Stock insuficiente en ingrediente '{}'. Stock actual: {}, Necesario: {}", 
                        ing.getNombre(), ing.getStockActual(), cantidadNecesaria);
                return false; // No hay suficiente ingrediente
            }
        }

        // Descontar todos los ingredientes
        log.debug("Descontando {} ingredientes de la receta", receta.getIngredientes().size());
        for (RecetaDetalle detalle : receta.getIngredientes()) {
            Ingrediente ing = detalle.getIngrediente();
            double cantidadNecesaria = detalle.getCantidadIngrediente() * cantidad;
            double stockAnterior = ing.getStockActual();
            
            ing.setStockActual(stockAnterior - cantidadNecesaria);
            ingredienteRepo.save(ing);
            
            log.trace("Ingrediente '{}' descontado: {}, Stock anterior: {}, Stock nuevo: {}", 
                     ing.getNombre(), cantidadNecesaria, stockAnterior, ing.getStockActual());
        }

        log.info("Ingredientes descontados exitosamente de receta '{}' para {} unidades", 
                receta.getNombre(), cantidad);
        return true;
    }

    private void validarProducto(Producto producto) {
        log.debug("Validando producto: '{}'", producto.getNombre());

        if (producto.isTieneReceta() && producto.getReceta() == null) {
            log.error("Validación fallida: Producto con receta pero sin receta asignada");
            throw new RuntimeException("Producto con receta debe tener una receta asignada");
        }
        if (!producto.isTieneReceta() && producto.getStock() < 0) {
            log.error("Validación fallida: Stock negativo para producto sin receta");
            throw new RuntimeException("Stock no puede ser negativo");
        }
        
        log.debug("Validación de producto exitosa");
    }

    private void calcularPreciosDesdeReceta(Producto producto) {
        if (producto.getReceta() != null) {
            double costoReceta = producto.getReceta().getCostoReceta();
            producto.setPrecioCompra(costoReceta);
            log.debug("Costo de receta establecido: {} para producto '{}'", costoReceta, producto.getNombre());

            // Si no se especificó precio de venta, calcular automático con 30% margen
            if (producto.getPrecioVenta() == 0) {
                double precioCalculado = costoReceta * 1.3;
                producto.setPrecioVenta(precioCalculado);
                log.debug("Precio de venta calculado automáticamente: {} (30% margen)", precioCalculado);
            } else {
                log.debug("Precio de venta especificado manualmente: {}", producto.getPrecioVenta());
            }
        }
    }
}