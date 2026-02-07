package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;

@Repository
public interface IngredienteRepositorio extends JpaRepository<Ingrediente, Long> {
        // Métodos con filtro empresa_id
        List<Ingrediente> findByEmpresaId(Long empresaId);

        Optional<Ingrediente> findByIdAndEmpresaId(Long id, Long empresaId);

        // Mantén métodos existentes pero agrega filtro empresa
        List<Ingrediente> findByNombreContainingIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

        //metodo para crear
        boolean existsByNombreAndEmpresa_Id(String nombre, Long empresaId);
        
        //metodo para editar
        boolean existsByNombreAndEmpresa_IdAndIdNot(String nombre, Long empresaId, Long id);


}
