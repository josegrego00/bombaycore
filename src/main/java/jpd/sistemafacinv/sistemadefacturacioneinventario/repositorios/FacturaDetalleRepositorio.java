package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaDetalle;

@Repository
public interface FacturaDetalleRepositorio
                extends JpaRepository<FacturaDetalle, Long> {

        @Query("SELECT COALESCE(SUM(rd.cantidadIngrediente * fd.cantidad), 0) " +
                        "FROM FacturaDetalle fd " +
                        "JOIN fd.producto p " +
                        "JOIN p.receta.ingredientes rd " +
                        "WHERE rd.ingrediente.id = :ingredienteId " +
                        "AND fd.factura.fecha BETWEEN :inicio AND :fin " +
                        "AND fd.factura.estado = 'PAGADA'")
        Double findConsumoByIngredienteAndFecha(
                        @Param("ingredienteId") Long ingredienteId,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin);

        // MÉTODO NUEVO para productos SIN receta
        @Query("SELECT COALESCE(SUM(fd.cantidad), 0) " +
                        "FROM FacturaDetalle fd " +
                        "WHERE fd.producto.id = :productoId " +
                        "AND fd.producto.tieneReceta = false " + // Solo productos sin receta
                        "AND fd.factura.fecha BETWEEN :inicio AND :fin " +
                        "AND fd.factura.estado = 'PAGADA'")
        Double findVentasProductoSinRecetaByFecha(
                        @Param("productoId") Long productoId,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin);
}