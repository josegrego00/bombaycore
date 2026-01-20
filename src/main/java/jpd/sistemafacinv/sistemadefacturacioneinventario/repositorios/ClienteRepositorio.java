package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;

@Repository
public interface ClienteRepositorio extends JpaRepository<Cliente, Integer> {

    List<Cliente> findByEmpresaId(Long empresaId);

    Optional<Cliente> findByIdAndEmpresaId(Long id, Long empresaId);

    // Búsqueda por nombre con empresa
    List<Cliente> findByNombreContainingIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    // Búsqueda por documento (RUC/DNI) con empresa
    Optional<Cliente> findByIdentificacionAndEmpresaId(String identificacion, Long empresaId);

    // Clientes activos por empresa
    List<Cliente> findByActivoTrueAndEmpresaId(Long empresaId);

    // Contar clientes por empresa
    Long countByEmpresaId(Long empresaId);

    boolean existsByNombreAndEmpresaId(String string, long empresaid);
}
