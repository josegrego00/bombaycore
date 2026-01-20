package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;

@Repository
public interface RecetaRepositorio extends JpaRepository<Receta, Integer> {

    // Métodos con filtro empresa_id
    List<Receta> findByEmpresaId(Long empresaId);

    Optional<Receta> findByIdAndEmpresaId(Long id, Long empresaId);
}
