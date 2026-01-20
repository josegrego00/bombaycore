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
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;

@Repository
public interface CierreInventarioDiarioRepositorio extends JpaRepository<CierreInventarioDiario, Long> {

        // Buscar cierre por fecha
        @Query("SELECT c FROM CierreInventarioDiario c WHERE DATE(c.fecha)= :fecha")
        Optional<CierreInventarioDiario> findByFecha(@Param("fecha") LocalDate fecha);

        // Cierres por estado
        List<CierreInventarioDiario> findByEstado(String estado);

        // Nuevo: Buscar cierre por fecha con estado COMPLETADO
        @Query("SELECT c FROM CierreInventarioDiario c WHERE c.fecha = :fecha AND c.estado = 'COMPLETADO'")
        CierreInventarioDiario findCierreCompletadoByFecha(@Param("fecha") LocalDate fecha);

        // NUEVO método con filtro empresa (agrégalo):
        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
                        "FROM CierreInventarioDiario c " +
                        "WHERE c.fecha = :fecha AND c.estado = :estado AND c.empresa.id = :empresaId")
        boolean existeCierrePorFechaYEstadoYEmpresaId(@Param("fecha") LocalDate fecha,
                        @Param("estado") String estado,
                        @Param("empresaId") Long empresaId);

        Optional<CierreInventarioDiario> findByFechaAndEmpresaId(LocalDate hoy, Long empresaId);

        Optional<CierreInventarioDiario> findByIdAndEmpresaId(Long cierreId, Long empresaId);

        List<CierreInventarioDiario> findByEmpresaId(long empresaId);

        Optional<CierreInventarioDiario> findByEstadoAndEmpresaId(String estado, Long empresaId);

        boolean existsByFechaAndEmpresaId(LocalDate hoy, Long id);

}
