package jpd.sistemafacinv.sistemadefacturacioneinventario.config;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ClienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmpresaRepositorio empresaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final CierreInventarioDiarioRepositorio cierreRepositorio;

    // Configuración desde application.properties
    @Value("${sistema.empresa.por-defecto.nombre:Sistema por Defecto}")
    private String nombreEmpresaDefault;

    @Value("${sistema.empresa.por-defecto.subdominio:defecto}")
    private String subdominioDefault;

    @Value("${sistema.empresa.por-defecto.crear-usuarios:true}")
    private boolean crearUsuarios;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🚀 INICIANDO CARGA DE DATOS INICIALES DEL SISTEMA");

        // 1. CREAR O OBTENER EMPRESA
        Empresa empresa = obtenerOCrearEmpresa();

        // 2. CREAR CLIENTE POR DEFECTO (si no existe)
        crearClientePorDefecto(empresa);

        // 3. CREAR USUARIOS POR DEFECTO (si no existen)
        if (crearUsuarios) {
            crearUsuariosPorDefecto(empresa);
        } else {
            log.info("⏭️  Configuración indica NO crear usuarios por defecto");
        }
        crearCierreDiarioInicial(empresa);
        
        crearSuperAdminSiNoExiste();

        log.info("✅ CARGA DE DATOS INICIALES COMPLETADA EXITOSAMENTE");
    }

    /**
     * 1. Obtener empresa existente o crear una nueva por defecto
     */
    private Empresa obtenerOCrearEmpresa() {
        if (empresaRepositorio.count() == 0) {
            log.info("📁 No se encontraron empresas. Creando empresa por defecto...");
            return crearEmpresaPorDefecto();
        } else {
            Empresa empresa = empresaRepositorio.findAll().get(0);
            log.info("✅ Empresa existente encontrada: {} (ID: {})",
                    empresa.getNombre(), empresa.getId());
            return empresa;
        }
    }

    /**
     * Crear empresa por defecto con configuración de application.properties
     */
    private Empresa crearEmpresaPorDefecto() {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombreEmpresaDefault);
        empresa.setSubdominio(subdominioDefault);
        empresa.setEstado(true);

        empresa = empresaRepositorio.save(empresa);
        log.info("🏢 Empresa por defecto creada exitosamente:");
        log.info("   📛 Nombre: {}", empresa.getNombre());
        log.info("   🌐 Subdominio: {}", empresa.getSubdominio());
        log.info("   🔢 ID: {}", empresa.getId());
        log.info("   🔗 Acceso: http://{}.localhost:8080", empresa.getSubdominio());

        return empresa;
    }

    /**
     * 2. Crear cliente "Consumidor Final" si no existe para la empresa
     */
    private void crearClientePorDefecto(Empresa empresa) {
        boolean existeCliente = clienteRepositorio.existsByNombreAndEmpresaId(
                "Consumidor Final", empresa.getId());

        if (!existeCliente) {
            Cliente cliente = new Cliente();
            cliente.setNombre("Consumidor Final");
            cliente.setIdentificacion("CF9999999999999");
            cliente.setTelefono("N/A");
            cliente.setCorreo("consumidor@final.com");
            cliente.setEmpresa(empresa);
            cliente.setActivo(true);

            clienteRepositorio.save(cliente);
            log.info("👤 Cliente por defecto creado:");
            log.info("   📛 Nombre: {}", cliente.getNombre());
            log.info("   🔢 ID: {}", cliente.getIdentificacion());
            log.info("   🏢 Para empresa: {}", empresa.getNombre());
        } else {
            log.info("✅ Cliente 'Consumidor Final' ya existe para empresa {}",
                    empresa.getNombre());
        }
    }

    /**
     * 3. Crear usuarios por defecto (admin, cajero, dev) si no existen
     */
    private void crearUsuariosPorDefecto(Empresa empresa) {
        // Verificar si ya existen usuarios para esta empresa
        long usuariosExistentes = usuarioRepositorio.countByEmpresaId(empresa.getId());

        if (usuariosExistentes == 0) {
            log.info("👥 No hay usuarios para empresa {}. Creando usuarios por defecto...",
                    empresa.getNombre());

            // Establecer tenant context para esta empresa
            TenantContext.setCurrentTenant(empresa.getId());

            try {
                // ADMIN
                crearUsuario("admin", "admin123", "ADMIN", empresa);

                // CAJERO
                crearUsuario("cajero", "cajero123", "CAJERO", empresa);

                // DEV
                crearUsuario("dev", "dev123", "DEV", empresa);

                log.info("✅ Usuarios creados exitosamente para empresa {}", empresa.getNombre());
                log.warn("⚠️  ¡IMPORTANTE! Cambia estas contraseñas en producción.");

            } finally {
                // Limpiar tenant context
                TenantContext.clear();
            }
        } else {
            log.info("✅ Ya existen {} usuarios para empresa {}. No se crean nuevos.",
                    usuariosExistentes, empresa.getNombre());
        }
    }

    /**
     * Método auxiliar para crear un usuario
     */
    private void crearUsuario(String username, String password, String rol,
            Empresa empresa) {
        Usuario usuario = Usuario.builder()
                .nombreUsuario(username)
                .contrasenna(passwordEncoder.encode(password))
                .rol(rol)
                .activo(true)
                .empresa(empresa)
                .build();

        usuarioRepositorio.save(usuario);

        log.info("   👤 {}: {}/{}", rol, username, password);
    }

    /**
     * Crear cierre diario inicial para la empresa (solo si no existe cierre para
     * hoy)
     */

    private void crearCierreDiarioInicial(Empresa empresa) {
        // Verificar si ya existe cierre para hoy para esta empresa
        LocalDate ayer = LocalDate.now().minusDays(1);
        boolean existeCierreHoy = cierreRepositorio.existsByFechaAndEmpresaId(ayer, empresa.getId());

        if (!existeCierreHoy) {
            // Obtener usuario admin para asociar al cierre
            Usuario usuarioAdmin = usuarioRepositorio.findByNombreUsuarioAndEmpresaId("admin", empresa.getId())
                    .orElse(null);

            if (usuarioAdmin == null) {
                log.warn("⚠️ No se encontró usuario 'admin' para crear cierre diario. Se creará sin usuario.");
            }

            CierreInventarioDiario cierre = new CierreInventarioDiario();
            cierre.setFecha(ayer);
            cierre.setEstado("COMPLETADO"); // O "PENDIENTE" según tu lógica
            cierre.setObservaciones("Cierre inicial generado automáticamente");
            cierre.setTotalVentas(0);
            cierre.setEmpresa(empresa);
            cierre.setUsuario(usuarioAdmin); // Puede ser null
            cierre.setCantidadFacturas(0);

            cierreRepositorio.save(cierre);
            log.info("📅 Cierre diario inicial creado para fecha {} - Empresa: {}",
                    ayer, empresa.getNombre());
        } else {
            log.info("✅ Ya existe cierre diario para hoy ({}) - Empresa: {}",
                    ayer, empresa.getNombre());
        }
    }

    // En DataInitializer.java, agrega este método:
    private void crearSuperAdminSiNoExiste() {
        log.info("👑 Verificando existencia de usuario SUPER_ADMIN");

        // Verificar si ya existe algún usuario con rol SUPER_ADMIN
        boolean existeSuperAdmin = usuarioRepositorio.existsByRol("SUPER_ADMIN");

        if (!existeSuperAdmin) {
            log.info("👑 No existe SUPER_ADMIN. Creando usuario maestro...");

            // Usuario SUPER_ADMIN (NO tiene empresa, es global)
            Usuario superAdmin = Usuario.builder()
                    .nombreUsuario("josepino")
                    .contrasenna(passwordEncoder.encode("123"))
                    .rol("SUPER_ADMIN")
                    .activo(true)
                    // ⚠️ IMPORTANTE: SUPER_ADMIN NO tiene empresa (null)
                    .empresa(null)
                    .build();

            usuarioRepositorio.save(superAdmin);
            log.warn("⚠️  USUARIO SUPER_ADMIN CREADO:");
            log.warn("   🎭 Rol: SUPER_ADMIN");
        } else {
            log.info("✅ Usuario SUPER_ADMIN ya existe en el sistema");
        }
    }

}