package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Proveedor;

@Repository
public interface ProveedorRepositorio extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByIdentificacion(String identificacion);

    List<Proveedor> findByEmpresaId(Long empresaId);

    // Verificar si existe identificación en una empresa específica
    boolean existsByIdentificacionAndEmpresaId(String identificacion, Long empresaId);

    // Buscar por nombre en una empresa
    List<Proveedor> findByNombreContainingIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    // Buscar por identificación y empresa (para validar duplicados al crear/editar)
    Optional<Proveedor> findByIdentificacionAndEmpresaId(String identificacion, Long empresaId);

    // Contar proveedores activos por empresa
    Long countByEmpresaIdAndActivoTrue(Long empresaId);
}
