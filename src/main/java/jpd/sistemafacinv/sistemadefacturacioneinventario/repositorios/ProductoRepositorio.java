package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Producto;

@Repository
public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    
    // Buscar productos SIN receta
    List<Producto> findByTieneRecetaFalse();

    // Buscar productos CON receta
    List<Producto> findByTieneRecetaTrue();

    // NUEVOS métodos con filtro empresa_id
    List<Producto> findByEmpresaId(Long empresaId);
    
    boolean existsByNombreAndEmpresa_Id(String nombre, Long empresaId);

    List<Producto> findByEmpresaIdAndTieneRecetaFalse(Long empresaId);

    List<Producto> findByEmpresaIdAndTieneRecetaTrue(Long empresaId);

    Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

}
