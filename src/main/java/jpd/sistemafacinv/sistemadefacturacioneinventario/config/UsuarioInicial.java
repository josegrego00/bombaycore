package jpd.sistemafacinv.sistemadefacturacioneinventario.config;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor

public class UsuarioInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicial.class);
    
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔧 Iniciando carga de usuarios iniciales");
        
        // Verificar si hay empresas
        if (empresaRepositorio.count() == 0) {
            log.warn("⚠️ No hay empresas en la BD. Los usuarios se crearán sin empresa.");
            log.warn("⚠️ Ejecuta primero el DataInitializer para crear empresa por defecto.");
            return;
        }
        
        // Obtener la primera empresa (generalmente la por defecto)
        Empresa empresa = empresaRepositorio.findAll().get(0);
        
        // Verificar usuarios por empresa
        long count = usuarioRepositorio.countByEmpresaId(empresa.getId());
        log.debug("Usuarios existentes en BD para empresa {}: {}", empresa.getNombre(), count);
        
        if (count == 0) {
            log.info("📝 No hay usuarios para empresa {}. Creando usuarios iniciales...", empresa.getNombre());
            
            // Establecer tenant context temporalmente
            TenantContext.setCurrentTenant(empresa.getId());
            
            try {
                // DEV
                Usuario dev = Usuario.builder()
                        .nombreUsuario("dev")
                        .contrasenna(passwordEncoder.encode("dev123"))
                        .rol("DEV")
                        .activo(true)
                        .empresa(empresa) // Asociar a empresa
                        .build();

                // CAJERO
                Usuario cajero = Usuario.builder()
                        .nombreUsuario("cajero")
                        .contrasenna(passwordEncoder.encode("cajero123"))
                        .rol("CAJERO")
                        .activo(true)
                        .empresa(empresa) // Asociar a empresa
                        .build();
                        
                // ADMIN
                Usuario administador = Usuario.builder()
                        .nombreUsuario("admin")
                        .contrasenna(passwordEncoder.encode("admin123"))
                        .rol("ADMIN")
                        .activo(true)
                        .empresa(empresa) // Asociar a empresa
                        .build();

                usuarioRepositorio.save(dev);
                usuarioRepositorio.save(cajero);
                usuarioRepositorio.save(administador);
                
                log.info("✅ Usuarios iniciales creados exitosamente para empresa {}:", empresa.getNombre());
                log.info("   👨‍💻 DEV: dev/dev123");
                log.info("   💰 CAJERO: cajero/cajero123");
                log.info("   👑 ADMIN: admin/admin123");
                log.warn("⚠️ ¡IMPORTANTE! Cambia estas contraseñas en producción.");
                
            } finally {
                TenantContext.clear();
            }
        } else {
            log.info("✅ Ya existen usuarios para empresa {}. No se crean nuevos.", empresa.getNombre());
        }
    }
}