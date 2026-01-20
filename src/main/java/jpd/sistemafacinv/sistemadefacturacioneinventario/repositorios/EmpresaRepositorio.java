package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;

public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findBySubdominio(String subdominio);

    boolean existsBySubdominio(String subdominio);

    
}
