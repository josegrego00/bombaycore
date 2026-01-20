package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DetalleCierreInventarioDiario;

@Repository
public interface DetalleCierreInventarioDiarioRepositorio extends JpaRepository<DetalleCierreInventarioDiario, Long> {

        // Buscar detalles de un cierre específico
        List<DetalleCierreInventarioDiario> findByCierre(CierreInventarioDiario cierre);

        // Método EXISTENTE (para stock inicial) - BUSCA < fecha
        @Query("SELECT d.ingrediente.id as ingredienteId, d.stockReal as stockReal " +
                        "FROM DetalleCierreInventarioDiario d " +
                        "WHERE d.cierre.estado = 'COMPLETADO' " +
                        "AND d.cierre.fecha = (" +
                        "    SELECT MAX(c.fecha) FROM CierreInventarioDiario c " +
                        "    WHERE c.estado = 'COMPLETADO' " +
                        "    AND c.fecha < :fecha" + // ← MENOR QUE (<)
                        ") " +
                        "AND d.ingrediente IS NOT NULL")
        List<StockInicialProjection> findStockInicialDeCierreAnterior(
                        @Param("fecha") LocalDate fecha);

        // Método NUEVO (para stock final) - BUSCA <= fecha
        @Query("SELECT d.ingrediente.id as ingredienteId, d.stockReal as stockReal " +
                        "FROM DetalleCierreInventarioDiario d " +
                        "WHERE d.cierre.estado = 'COMPLETADO' " +
                        "AND d.cierre.fecha = (" +
                        "    SELECT MAX(c.fecha) FROM CierreInventarioDiario c " +
                        "    WHERE c.estado = 'COMPLETADO' " +
                        "    AND c.fecha <= :fecha" + // ← MENOR O IGUAL (<=)
                        ") " +
                        "AND d.ingrediente IS NOT NULL")
        List<StockInicialProjection> findStockRealFinalHastaFecha(
                        @Param("fecha") LocalDate fecha);

        // En DetalleCierreRepository
        Optional<DetalleCierreInventarioDiario> findByIdAndCierreEmpresaId(
                        Long detalleId, Long empresaId);

 //       Optional<DetalleCierreInventarioDiario> findByFechaAndEmpresaId(LocalDate hoy, Long empresaId);
}
