package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Factura;

@Repository
public interface FacturaRepositorio extends JpaRepository<Factura, Integer> {

        // Ventas del día actual
        @Query("SELECT f FROM Factura f WHERE DATE(f.fecha) = CURRENT_DATE AND f.estado = 'PAGADA'")
        List<Factura> findFacturasHoy();

        // Obtener total vendido hoy
        @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f WHERE DATE(f.fecha) = CURRENT_DATE AND f.estado = 'PAGADA'")
        Double getTotalVentasHoy();

        List<Factura> findByFechaAndEstado(LocalDate fecha, String estado);// esta hay q cambiarla en el metodo

        // Métodos básicos con empresa
        List<Factura> findByEmpresaId(Long empresaId);

        Optional<Factura> findByIdAndEmpresaId(Integer id, Long empresaId);

        // Métodos existentes modificados:
        // Ventas del día actual POR EMPRESA
        @Query("SELECT f FROM Factura f WHERE f.empresa.id = :empresaId AND DATE(f.fecha) = CURRENT_DATE AND f.estado = 'PAGADA'")
        List<Factura> findFacturasHoy(@Param("empresaId") Long empresaId);

        // Total vendido hoy POR EMPRESA
        @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f WHERE f.empresa.id = :empresaId AND DATE(f.fecha) = CURRENT_DATE AND f.estado = 'PAGADA'")
        Double getTotalVentasHoy(@Param("empresaId") Long empresaId);

        // Buscar por fecha y estado POR EMPRESA
        List<Factura> findByFechaAndEstadoAndEmpresaId(LocalDate fecha, String estado, Long empresaId);

        // Contar facturas POR EMPRESA (para número de factura)
        Long countByEmpresaId(Long empresaId);

        @Query("SELECT DISTINCT f FROM Factura f LEFT JOIN FETCH f.facturaDetalle WHERE f.empresa.id = :empresaId")
        List<Factura> findByEmpresaIdWithDetalle(@Param("empresaId") Long empresaId);

        @Query("SELECT f FROM Factura f " +
                        "LEFT JOIN FETCH f.facturaDetalle " +
                        "WHERE f.id = :id AND f.empresa.id = :empresaId")
        Optional<Factura> findByIdWithDetalle(
                        @Param("id") Long id,
                        @Param("empresaId") Long empresaId);

        // Método base con paginación
        @Query("SELECT f FROM Factura f WHERE f.empresa.id = :empresaId")
        Page<Factura> findByEmpresaId(
                        @Param("empresaId") Long empresaId,
                        Pageable pageable);

        // Con filtros adicionales
        @Query("""
                            SELECT f FROM Factura f
                            WHERE f.empresa.id = :empresaId
                            AND (:estado IS NULL OR f.estado = :estado)
                            AND (:fechaInicio IS NULL OR f.fecha >= :fechaInicio)
                            AND (:fechaFin IS NULL OR f.fecha <= :fechaFin)
                            ORDER BY f.fecha DESC
                        """)
        Page<Factura> buscarConFiltros(
                        @Param("empresaId") Long empresaId,
                        @Param("estado") String estado,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin,
                        Pageable pageable);
}
