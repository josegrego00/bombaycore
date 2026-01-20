package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.ItemConsumoDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.ReporteConsumoDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.DetalleCierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.FacturaDetalleProveedorRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.FacturaDetalleRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProductoRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.StockInicialProjection;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReporteConsumoServicio {

    private static final Logger log = LoggerFactory.getLogger(ReporteConsumoServicio.class);

    private final DetalleCierreInventarioDiarioRepositorio detalleCierreRepo;
    private final IngredienteRepositorio ingredienteRepo;
    private final FacturaDetalleProveedorRepositorio compraRepo;
    private final FacturaDetalleRepositorio ventaRepo;
    private final ProductoRepositorio productoRepo;

    public ReporteConsumoDTO generarReporte(LocalDate inicio, LocalDate fin) {
        log.info("Generando reporte de consumo desde {} hasta {}", inicio, fin);

        // 1. Stock inicial del cierre anterior
        Map<Long, Double> stockInicialMap = new HashMap<>();
        List<StockInicialProjection> stocksIniciales = detalleCierreRepo.findStockInicialDeCierreAnterior(inicio);

        log.debug("Stocks iniciales encontrados: {} registros", stocksIniciales.size());

        for (StockInicialProjection proyeccion : stocksIniciales) {
            stockInicialMap.put(proyeccion.getIngredienteId(), proyeccion.getStockReal());
            log.trace("Stock inicial - Ingrediente ID: {}, Stock: {}", proyeccion.getIngredienteId(),
                    proyeccion.getStockReal());
        }

        log.info("📊 Stocks iniciales encontrados: {}", stocksIniciales.size());

        // 2. Stock real final (al final del período)
        Map<Long, Double> stockFinalMap = new HashMap<>();
        List<StockInicialProjection> stocksFinales = detalleCierreRepo.findStockRealFinalHastaFecha(fin);

        log.debug("Stocks finales encontrados: {} registros", stocksFinales.size());

        for (StockInicialProjection proyeccion : stocksFinales) {
            stockFinalMap.put(proyeccion.getIngredienteId(), proyeccion.getStockReal());
            log.trace("Stock final - Ingrediente ID: {}, Stock: {}", proyeccion.getIngredienteId(),
                    proyeccion.getStockReal());
        }

        // 3. Para cada ingrediente, crear ItemConsumoDTO
        List<Ingrediente> todosIngredientes = ingredienteRepo.findAll();
        List<Producto> todoProductoSinReceta = productoRepo.findByTieneRecetaFalse();

        log.debug("Total ingredientes: {}, Productos sin receta: {}",
                todosIngredientes.size(), todoProductoSinReceta.size());

        List<ItemConsumoDTO> items = new ArrayList<>();
        Double totalValorDiferencia = 0.0;
        Double totalConsumoValor = 0.0;

        log.info("Procesando {} ingredientes", todosIngredientes.size());
        for (Ingrediente ing : todosIngredientes) {
            ItemConsumoDTO item = new ItemConsumoDTO();
            item.setIngredienteId(ing.getId());
            item.setNombreIngrediente(ing.getNombre());
            item.setUnidadMedida(ing.getUnidadMedida());
            Double costoPromedio = ing.getPrecio();
            // Double costoPromedio = calcularCostoPromedio(ing.getId(), inicio);
            item.setCostoUnitarioPromedio(costoPromedio);

            // Stock inicial (0 si no hay registro)
            Double stockInicial = stockInicialMap.get(ing.getId());
            item.setStockInicial(stockInicial != null ? stockInicial : 0.0);

            // Compras en el período
            Double compras = compraRepo.findComprasByIngredienteAndFecha(
                    ing.getId(), inicio, fin);
            item.setComprasPeriodo(compras != null ? compras : 0.0);

            // Consumo por ventas en el período
            Double consumo = ventaRepo.findConsumoByIngredienteAndFecha(
                    ing.getId(), inicio, fin);
            item.setConsumoVentas(consumo != null ? consumo : 0.0);

            // Stock real final (0 si no hay cierre en esa fecha)
            Double stockRealFinal = stockFinalMap.get(ing.getId());
            item.setStockRealFinal(stockRealFinal != null ? stockRealFinal : 0.0);

            // CÁLCULOS
            // Stock teórico final = inicial + compras - consumo
            Double stockTeorico = item.getStockInicial()
                    + item.getComprasPeriodo()
                    - item.getConsumoVentas();
            item.setStockTeoricoFinal(stockTeorico);

            // Diferencia = real - teórico
            Double diferencia = item.getStockRealFinal() - stockTeorico;
            item.setDiferencia(diferencia);

            // Valor diferencia = diferencia × costo unitario
            Double valorDiferencia = diferencia * costoPromedio;
            item.setValorDiferencia(valorDiferencia);

            // Acumular totales
            totalValorDiferencia += valorDiferencia;
            totalConsumoValor += (item.getConsumoVentas() * costoPromedio);

            // Agregar a la lista
            items.add(item);

            log.trace("Ingrediente procesado: {} (ID: {}), Diferencia: {}, Valor: {}",
                    ing.getNombre(), ing.getId(), diferencia, valorDiferencia);
        }

        // Agregar productos sin receta (solo para mostrar en el reporte)
        log.info("Procesando {} productos sin receta", todoProductoSinReceta.size());
        for (Producto producto : todoProductoSinReceta) {
            ItemConsumoDTO item = new ItemConsumoDTO();
            item.setIngredienteId(producto.getId());
            item.setNombreIngrediente(producto.getNombre());
            item.setUnidadMedida("Unidad");

            // Para productos sin receta, el costo es el precio de venta
            double costo = producto.getPrecioVenta() != null ? producto.getPrecioVenta() : 0.0;
            item.setCostoUnitarioPromedio(costo);

            // Stock inicial (usar stock del producto)
            item.setStockInicial(producto.getStock() != null ? producto.getStock() : 0.0);

            // Compras: productos sin receta no tienen compras de ingredientes
            item.setComprasPeriodo(0.0);

            // Consumo por ventas
            Double consumo = ventaRepo
                    .findVentasProductoSinRecetaByFecha(producto.getId(), inicio, fin);
            item.setConsumoVentas(consumo != null ? consumo : 0.0);

            // Calcular stock teórico final
            Double stockTeorico = item.getStockInicial()
                    + item.getComprasPeriodo()
                    - item.getConsumoVentas();
            item.setStockTeoricoFinal(stockTeorico);

            // Stock real final = stock actual del producto
            item.setStockRealFinal(producto.getStock() != null ? producto.getStock() : 0.0);

            // Diferencia
            Double diferencia = item.getStockRealFinal() - stockTeorico;
            item.setDiferencia(diferencia);

            // Valor diferencia
            Double valorDiferencia = diferencia * costo;
            item.setValorDiferencia(valorDiferencia);

            // Agregar a la lista
            items.add(item);

            // Acumular totales
            totalValorDiferencia += valorDiferencia;
            totalConsumoValor += (item.getConsumoVentas() * costo);

            log.trace("Producto sin receta procesado: {} (ID: {}), Diferencia: {}, Valor: {}",
                    producto.getNombre(), producto.getId(), diferencia, valorDiferencia);
        }

        // 4. Crear ReporteConsumoDTO
        ReporteConsumoDTO reporte = new ReporteConsumoDTO();
        reporte.setFechaInicio(inicio);
        reporte.setFechaFin(fin);
        reporte.setItems(items);
        reporte.setTotalValorDiferencia(totalValorDiferencia);
        reporte.setTotalIngredientesAnalizados(items.size());

        // Calcular porcentaje de diferencia
        if (totalConsumoValor > 0) {
            Double porcentajeDiferencia = (totalValorDiferencia / totalConsumoValor) * 100;
            reporte.setPorcentajeDiferenciaTotal(porcentajeDiferencia);
            log.debug("Porcentaje de diferencia calculado: {}%", porcentajeDiferencia);
        } else {
            reporte.setPorcentajeDiferenciaTotal(0.0);
            log.warn("Total consumo valor es 0, porcentaje de diferencia establecido en 0");
        }

        log.info("Reporte generado exitosamente. Total items: {}, Diferencia total: {}, Porcentaje: {}%",
                items.size(), totalValorDiferencia, reporte.getPorcentajeDiferenciaTotal());

        return reporte;
    }

    private Double calcularCostoPromedio(Long ingredienteId, LocalDate fecha) {
        log.debug("Calculando costo promedio para ingrediente ID: {} hasta fecha: {}", ingredienteId, fecha);
        Double costo = compraRepo.findCostoPromedioHastaFecha(ingredienteId, fecha);
        Double resultado = costo != null ? costo : 0.0;
        log.trace("Costo promedio resultado: {} para ingrediente ID: {}", resultado, ingredienteId);
        return resultado; // ← SIEMPRE retorna un número
    }
}