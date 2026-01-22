package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;

@Repository
@Transactional
public interface RecetaRepositorio extends JpaRepository<Receta, Integer> {

    // Métodos con filtro empresa_id
    List<Receta> findByEmpresaId(Long empresaId);

    Optional<Receta> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query("SELECT DISTINCT r FROM Receta r LEFT JOIN FETCH r.ingredientes WHERE r.empresa.id = :empresaId")
    List<Receta> findByEmpresaIdWithIngredientes(@Param("empresaId") Long empresaId);

    @Query("SELECT r FROM Receta r " +
            "LEFT JOIN FETCH r.ingredientes " +
            "WHERE r.id = :id AND r.empresa.id = :empresaId")
    Optional<Receta> findByIdWithIngredientes(
            @Param("id") Long id,
            @Param("empresaId") Long empresaId);
}
