package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.EmpresaClienteDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ClienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuperAdminService {

    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final CierreInventarioDiarioRepositorio cierreRepositorio;

    @Transactional
    public Empresa crearEmpresaCliente(EmpresaClienteDTO dto) {
        log.info("🚀 [SUPERADMIN] Iniciando creación de nueva empresa cliente");
        log.info("📋 Datos recibidos - Nombre: {}, Subdominio: {}, Email: {}",
                dto.getNombre(), dto.getSubdominio(), dto.getEmailContacto());

        // 1. Validar subdominio único
        log.debug("🔍 Validando subdominio: {}", dto.getSubdominio());
        validarSubdominioUnico(dto.getSubdominio());
        log.info("✅ Subdominio disponible");

        // 2. Crear empresa
        log.debug("🏗️ Creando empresa desde DTO");
        Empresa empresa = crearEmpresaDesdeDTO(dto);
        log.info("✅ Empresa creada: {} (ID: {})", empresa.getNombre(), empresa.getId());

        // 3. Crear usuario admin si está configurado
        if (dto.isCrearUsuarios()) {
            log.info("👤 Creando usuario administrador...");
            crearUsuarioAdminParaEmpresa(empresa);
            log.info("✅ Usuario admin creado para empresa {}", empresa.getNombre());
        } else {
            log.info("⏭️  Omitiendo creación de usuario (configuración)");
        }

        // 4. Crear datos iniciales si está configurado
        if (dto.isCrearDatosIniciales()) {
            log.info("📦 Creando datos iniciales...");
            crearDatosIniciales(empresa);
            log.info("✅ Datos iniciales creados para empresa {}", empresa.getNombre());
        } else {
            log.info("⏭️  Omitiendo datos iniciales (configuración)");
        }
        // 4. NUEVO: Crear cierre diario por defecto
        crearCierreDiarioPorDefecto(empresa);
        // 5. Asignar plan (si tienes sistema de planes)
        log.debug("📋 Asignando plan: {}", dto.getPlan());
        asignarPlan(empresa, dto.getPlan());
        log.info("💰 Plan '{}' asignado a empresa {}", dto.getPlan(), empresa.getNombre());

        log.info("🎉 Creación de empresa cliente completada exitosamente");
        return empresa;
    }

    private void crearCierreDiarioPorDefecto(Empresa empresa) {

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

    private void validarSubdominioUnico(String subdominio) {
        log.trace("🔎 Verificando subdominio en BD: {}", subdominio);
        if (empresaRepositorio.existsBySubdominio(subdominio)) {
            log.error("❌ Subdominio '{}' ya está en uso", subdominio);
            throw new RuntimeException("El subdominio '" + subdominio + "' ya está en uso");
        }
        log.trace("✅ Subdominio disponible");
    }

    private Empresa crearEmpresaDesdeDTO(EmpresaClienteDTO dto) {
        log.trace("⚙️ Construyendo entidad Empresa");
        Empresa empresa = new Empresa();
        empresa.setNombre(dto.getNombre());
        empresa.setSubdominio(dto.getSubdominio().toLowerCase());
        empresa.setEmailContacto(dto.getEmailContacto());
        empresa.setTelefono(dto.getTelefono());
        empresa.setEstado(true);
        empresa.setFechaCreacion(LocalDate.now());

        log.debug("💾 Guardando empresa en BD");
        Empresa saved = empresaRepositorio.save(empresa);
        log.trace("✅ Empresa guardada con ID: {}", saved.getId());

        return saved;
    }

    private void crearUsuarioAdminParaEmpresa(Empresa empresa) {
        log.trace("👤 Construyendo usuario administrador");
        Usuario admin = Usuario.builder()
                .nombreUsuario("admin")
                .contrasenna(passwordEncoder.encode("admin123"))
                .rol("ADMIN")
                .activo(true)
                .empresa(empresa)
                .build();

        log.debug("💾 Guardando usuario admin en BD");
        usuarioRepositorio.save(admin);
        log.info("👤 Usuario creado - Username: admin, Empresa: {}", empresa.getNombre());
    }

    private void crearDatosIniciales(Empresa empresa) {
        log.trace("📦 Creando cliente por defecto");
        Cliente cliente = new Cliente();
        cliente.setNombre("Consumidor Final");
        cliente.setIdentificacion("CF9999999999999");
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);

        log.debug("💾 Guardando cliente en BD");
        clienteRepositorio.save(cliente);
        log.info("👤 Cliente 'Consumidor Final' creado para empresa {}", empresa.getNombre());
    }

    private void asignarPlan(Empresa empresa, String plan) {
        log.trace("📋 Procesando asignación de plan: {}", plan);
        // Aquí implementarías la lógica de planes
        log.debug("💰 Plan '{}' procesado para empresa {}", plan, empresa.getNombre());
    }
}