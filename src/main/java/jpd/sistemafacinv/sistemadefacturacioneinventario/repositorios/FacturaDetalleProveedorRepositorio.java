package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaProveedor;

public interface FacturaDetalleProveedorRepositorio extends JpaRepository<FacturaProveedor, Long> {

        @Query("SELECT COALESCE(SUM(fpd.cantidad), 0) " +
                        "FROM FacturaDetalleProveedor fpd " +
                        "WHERE fpd.ingrediente.id = :ingredienteId " +
                        "AND fpd.facturaProveedor.fecha BETWEEN :inicio AND :fin")
        Double findComprasByIngredienteAndFecha(
                        @Param("ingredienteId") Long ingredienteId,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin);

        @Query("SELECT COALESCE(AVG(fpd.precioUnitario), 0) " +
                        "FROM FacturaDetalleProveedor fpd " +
                        "WHERE fpd.ingrediente.id = :ingredienteId " +
                        "AND fpd.facturaProveedor.fecha <= :fecha")
        Double findCostoPromedioHastaFecha(
                        @Param("ingredienteId") Long ingredienteId,
                        @Param("fecha") LocalDate fecha);
}
