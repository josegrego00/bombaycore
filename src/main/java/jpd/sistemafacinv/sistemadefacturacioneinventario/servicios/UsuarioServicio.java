package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantFilter;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.UsuarioRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UsuarioServicio implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServicio.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepositorio empresaRepositorio;

    // metodo para crear la autenticacion del usuario
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.debug("Autenticando usuario: {} para la empresa ID: {}", username, TenantContext.getCurrentTenant());

        // 1. Obtener request actual
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.error("No hay contexto de request disponible para autenticar usuario: {}", username);
            throw new UsernameNotFoundException("No hay contexto de request");
        }

        HttpServletRequest request = attributes.getRequest();
        String host = request.getServerName(); // "bar.localhost"
        // 2. Extraer subdominio
        String subdominio = TenantFilter.extraerSubdominio(host);

        log.info("🔐 Login - Usuario: {} | Subdominio: {}", username, subdominio);

        // Check if it's SUPER_ADMIN
        if (isSuperAdminLogin(request)) {
            // Handle SUPER_ADMIN without company
            return loadSuperAdmin(username);
        }

        // 3. Buscar empresa
        Empresa empresa = empresaRepositorio.findBySubdominio(subdominio)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada para subdominio: {}", subdominio);
                    return new UsernameNotFoundException("Empresa no encontrada para: " + subdominio);
                });

        // 4. DEBUG COMPLETO ← AQUÍ
        log.debug("🔐 Login - Usuario: {} | Subdominio: {} | Empresa ID: {} | Empresa Nombre: {}",
                username, subdominio, empresa.getId(), empresa.getNombre());

        Usuario user = usuarioRepositorio.buscarPorUsuarioYEmpresa(username, empresa.getId())
                .orElseThrow(() -> {
                    log.warn("Credenciales no encontradas para usuario: {} en empresa ID: {}", username,
                            empresa.getId());
                    return new UsernameNotFoundException("Credenciales no encontradas");
                });

        log.info("🔐 Encontrado usuario: {}", user.getNombreUsuario());
        log.info("🔐 Rol: {}", user.getRol());
        log.debug("🔐 Activo: {}", user.isActivo());

        return User.builder()
                .username(user.getNombreUsuario())
                .password(user.getContrasenna())
                .roles(user.getRol())
                .disabled(!user.isActivo())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .build();
    }
    // metodos adicionales para el servicio de usuario.

    private UserDetails loadSuperAdmin(String username) {
        // Find user WITHOUT company check
        Usuario user = usuarioRepositorio.findByNombreUsuario(username)
                .orElseThrow(() -> {
                    log.error("SUPER_ADMIN no encontrado: {}", username);
                    return new UsernameNotFoundException("SUPER_ADMIN no encontrado");
                });

        // Verify it's SUPER_ADMIN role
        if (!"SUPER_ADMIN".equals(user.getRol())) {
            log.error("Usuario {} no es SUPER_ADMIN, tiene rol: {}", username, user.getRol());
            throw new UsernameNotFoundException("No es SUPER_ADMIN");
        }

        log.info("✅ SUPER_ADMIN cargado: {} - Rol: {}", username, user.getRol());

        return User.builder()
                .username(user.getNombreUsuario())
                .password(user.getContrasenna())
                .roles(user.getRol())
                .disabled(!user.isActivo())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .build();
    }

    private boolean isSuperAdminLogin(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/superadmin/");
    }

    // Solo ADMIN y DEV pueden crear usuarios
    public Usuario crearUsuario(Usuario usuario) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Creando usuario: {} para empresa ID: {}", usuario.getNombreUsuario(), empresaId);

        // Validar nombre único EN ESTA EMPRESA
        if (usuarioRepositorio.existsByNombreUsuarioAndEmpresaId(
                usuario.getNombreUsuario(), empresaId)) {
            log.warn("Usuario {} ya existe en empresa ID: {}", usuario.getNombreUsuario(), empresaId);
            throw new RuntimeException("El usuario ya existe en esta empresa");
        }

        // Encriptar y asignar empresa
        usuario.setContrasenna(passwordEncoder.encode(usuario.getContrasenna()));
        Empresa empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada ID: {}", empresaId);
                    return new RuntimeException("Empresa no encontrada");
                });
        usuario.setEmpresa(empresa);
        usuario.setActivo(true);

        Usuario usuarioCreado = usuarioRepositorio.save(usuario);
        log.info("Usuario creado exitosamente: {} con ID: {}", usuarioCreado.getNombreUsuario(), usuarioCreado.getId());
        return usuarioCreado;
    }

    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Actualizando usuario ID: {} en empresa ID: {}", id, empresaId);

        // Buscar usuario en esta empresa
        Usuario usuario = usuarioRepositorio.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado ID: {} en empresa ID: {}", id, empresaId);
                    return new RuntimeException("Usuario no encontrado");
                });

        // Validar nombre único SI CAMBIA
        if (!usuario.getNombreUsuario().equals(usuarioActualizado.getNombreUsuario()) &&
                usuarioRepositorio.existsByNombreUsuarioAndEmpresaId(
                        usuarioActualizado.getNombreUsuario(), empresaId)) {
            log.warn("Nombre de usuario {} ya existe en empresa ID: {}", usuarioActualizado.getNombreUsuario(),
                    empresaId);
            throw new RuntimeException("Ya existe otro usuario con este nombre en la empresa");
        }

        usuario.setNombreUsuario(usuarioActualizado.getNombreUsuario());
        usuario.setRol(usuarioActualizado.getRol());
        usuario.setActivo(usuarioActualizado.isActivo());

        // Solo actualizar password si se proporciona
        if (usuarioActualizado.getContrasenna() != null &&
                !usuarioActualizado.getContrasenna().isEmpty()) {
            log.debug("Actualizando contraseña para usuario ID: {}", id);
            usuario.setContrasenna(passwordEncoder.encode(usuarioActualizado.getContrasenna()));
        }

        Usuario usuarioActualizadoObj = usuarioRepositorio.save(usuario);
        log.info("Usuario actualizado exitosamente: {} (ID: {})", usuarioActualizadoObj.getNombreUsuario(), id);
        return usuarioActualizadoObj;
    }

    // 🔄 LISTAR USUARIOS MULTI-TENANT
    public List<Usuario> listarUsuarios() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando usuarios para empresa ID: {}", empresaId);
        return usuarioRepositorio.findByEmpresaId(empresaId);
    }

    // 🔄 OBTENER USUARIO MULTI-TENANT
    public Usuario obtenerUsuario(Long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Obteniendo usuario ID: {} para empresa ID: {}", id, empresaId);
        return usuarioRepositorio.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado ID: {} en empresa ID: {}", id, empresaId);
                    return new RuntimeException("Usuario no encontrado");
                });
    }

    public void eliminarUsuario(Long id) {
        log.debug("Eliminando (lógicamente) usuario ID: {}", id);
        Usuario usuario = obtenerUsuario(id);
        usuario.setActivo(false); // Eliminación lógica
        usuarioRepositorio.save(usuario);
        log.info("Usuario eliminado lógicamente: {} (ID: {})", usuario.getNombreUsuario(), id);
    }

    // 🔄 BUSCAR POR NOMBRE MULTI-TENANT
    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando usuario: {} en empresa ID: {}", nombreUsuario, empresaId);
        return usuarioRepositorio.buscarPorUsuarioYEmpresa(nombreUsuario, empresaId)
                .orElse(null);
    }
    /*
     * public static void main(String[] args) {
     * //para ver el bycrypt en system
     * System.out.println( new BCryptPasswordEncoder().encode("dev123") );
     * }
     */
}