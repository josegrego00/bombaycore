package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.RecetaDetalle;

@Repository
public interface RecetaDetalleRepositorio extends JpaRepository<RecetaDetalle, Integer>{

    
}
