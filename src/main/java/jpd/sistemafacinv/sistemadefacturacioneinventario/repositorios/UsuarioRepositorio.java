package jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    // 🔄 AGREGAR ESTOS MÉTODOS MULTI-TENANT:

    // Buscar usuario por ID y empresa
    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    // Listar usuarios por empresa
    List<Usuario> findByEmpresaId(Long empresaId);

    // Verificar si existe nombre de usuario en una empresa específica
    boolean existsByNombreUsuarioAndEmpresaId(String nombreUsuario, Long empresaId);

    /*
     * // Buscar usuario por nombre de usuario Y empresa
     * Optional<Usuario> findByNombreUsuarioAndEmpresa_Id(String nombreUsuario, Long
     * empresaId);
     */

    @Query("SELECT u FROM Usuario u WHERE u.nombreUsuario = :usuario AND u.empresa.id = :empresaId")
    Optional<Usuario> buscarPorUsuarioYEmpresa(
            @Param("usuario") String usuario,
            @Param("empresaId") Long empresaId);

    // Contar usuarios por empresa y rol
    Long countByEmpresaIdAndRol(Long empresaId, String rol);

    // Buscar usuarios por rol en una empresa
    List<Usuario> findByEmpresaIdAndRol(Long empresaId, String rol);

    
    long countByEmpresaId(Long empresaId);

    Optional<Usuario> findByNombreUsuarioAndEmpresaId(String nombreUsuario, Long empresaId);

    boolean existsByRol(String string);
}