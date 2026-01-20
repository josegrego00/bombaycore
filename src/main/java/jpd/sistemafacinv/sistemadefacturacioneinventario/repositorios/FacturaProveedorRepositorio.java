package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.FacturaProveedor;

@Repository
public interface FacturaProveedorRepositorio extends JpaRepository<FacturaProveedor, Long> {

    List<FacturaProveedor> findByOrderByFechaDesc();

    FacturaProveedor findByNumeroFactura(String numeroFactura);

    // Listar todas las facturas de una empresa, ordenadas por fecha descendente
    List<FacturaProveedor> findByEmpresaIdOrderByFechaDesc(Long empresaId);

    // Buscar factura por ID y empresa
    Optional<FacturaProveedor> findByIdAndEmpresaId(Integer id, Long empresaId);

    // Buscar factura por número de factura y empresa
    Optional<FacturaProveedor> findByNumeroFacturaAndEmpresaId(String numeroFactura, Long empresaId);

    // Listar facturas por proveedor específico dentro de una empresa
    List<FacturaProveedor> findByProveedorIdAndEmpresaId(Integer proveedorId, Long empresaId);

    // Listar facturas por rango de fechas y empresa
    List<FacturaProveedor> findByFechaBetweenAndEmpresaId(LocalDate fechaInicio, LocalDate fechaFin, Long empresaId);

    // Verificar si existe número de factura en una empresa
    boolean existsByNumeroFacturaAndEmpresaId(String numeroFactura, Long empresaId);

    // ===== MÉTODOS PARA REPORTES =====

    // Calcular total de compras por mes y empresa
    @Query("SELECT COALESCE(SUM(fp.total), 0) FROM FacturaProveedor fp " +
            "WHERE YEAR(fp.fecha) = :year AND MONTH(fp.fecha) = :month " +
            "AND fp.empresa.id = :empresaId")
    Double calcularTotalComprasPorMes(
            @Param("year") int year,
            @Param("month") int month,
            @Param("empresaId") Long empresaId);

    // Contar facturas por estado y empresa
    Long countByEstadoAndEmpresaId(String estado, Long empresaId);

    Long countByEmpresaId(Long empresaId);
}
