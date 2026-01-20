package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Proveedor;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ProveedorRepositorio;
import lombok.AllArgsConstructor;

import java.util.List;

@Service
@AllArgsConstructor
public class ProveedorServicio {

    private static final Logger log = LoggerFactory.getLogger(ProveedorServicio.class);

    private final ProveedorRepositorio repositorio;
    private final EmpresaRepositorio empresaRepositorio;

    // CRUD básico
    public List<Proveedor> listarProveedores() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando proveedores para empresa ID: {}", empresaId);

        List<Proveedor> proveedores = repositorio.findByEmpresaId(empresaId);
        log.debug("Encontrados {} proveedores para empresa ID: {}", proveedores.size(), empresaId);

        return proveedores;
    }

    public Proveedor buscarProveedor(Integer id) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando proveedor ID: {} para empresa ID: {}", id, empresaId);

        return repositorio.findByIdAndEmpresaId(id.longValue(), empresaId)
                .orElseThrow(() -> {
                    log.error("Proveedor no encontrado ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Proveedor no encontrado");
                });
    }

    public Proveedor guardarProveedor(Proveedor proveedor) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Guardando proveedor '{}' para empresa ID: {}", proveedor.getNombre(), empresaId);

        // Validar identificación única EN ESTA EMPRESA
        if (proveedor.getIdentificacion() != null && !proveedor.getIdentificacion().isEmpty()) {
            log.debug("Validando identificación única: {} para empresa ID: {}",
                    proveedor.getIdentificacion(), empresaId);

            boolean existe = repositorio.existsByIdentificacionAndEmpresaId(
                    proveedor.getIdentificacion(), empresaId);

            if (existe && (proveedor.getId() == null ||
                    !repositorio.findByIdAndEmpresaId(proveedor.getId(), empresaId)
                            .map(p -> p.getIdentificacion().equals(proveedor.getIdentificacion()))
                            .orElse(false))) {
                log.warn("Identificación duplicada: {} para empresa ID: {}",
                        proveedor.getIdentificacion(), empresaId);
                throw new RuntimeException("Ya existe un proveedor con esa identificación en esta empresa");
            }
        }

        Empresa empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada ID: {}", empresaId);
                    return new RuntimeException("Empresa no encontrada");
                });

        // Asignar empresa
        proveedor.setEmpresa(empresa);
        proveedor.setActivo(true);

        Proveedor proveedorGuardado = repositorio.save(proveedor);
        log.info("Proveedor guardado exitosamente: '{}' (ID: {}) para empresa ID: {}",
                proveedorGuardado.getNombre(), proveedorGuardado.getId(), empresaId);

        return proveedorGuardado;
    }

    public Proveedor actualizarProveedor(Integer id, Proveedor proveedorActualizado) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Actualizando proveedor ID: {} para empresa ID: {}", id, empresaId);

        // Buscar proveedor (ya filtra por empresa)
        Proveedor proveedorExistente = buscarProveedor(id);

        if (proveedorActualizado.getIdentificacion() != null &&
                !proveedorActualizado.getIdentificacion().equals(proveedorExistente.getIdentificacion())) {

            log.debug("Validando nueva identificación: {} para empresa ID: {}",
                    proveedorActualizado.getIdentificacion(), empresaId);

            boolean existe = repositorio.existsByIdentificacionAndEmpresaId(
                    proveedorActualizado.getIdentificacion(), empresaId);

            if (existe) {
                log.warn("Identificación duplicada: {} para empresa ID: {}",
                        proveedorActualizado.getIdentificacion(), empresaId);
                throw new RuntimeException("Ya existe otro proveedor con esa identificación en esta empresa");
            }
        }

        log.debug("Actualizando campos del proveedor ID: {}", id);
        // Actualizar campos
        proveedorExistente.setNombre(proveedorActualizado.getNombre());
        proveedorExistente.setIdentificacion(proveedorActualizado.getIdentificacion());
        proveedorExistente.setTelefono(proveedorActualizado.getTelefono());
        proveedorExistente.setCorreo(proveedorActualizado.getCorreo());
        proveedorExistente.setActivo(proveedorActualizado.getActivo());

        Proveedor proveedorActualizadoObj = repositorio.save(proveedorExistente);
        log.info("Proveedor actualizado exitosamente: '{}' (ID: {}) para empresa ID: {}",
                proveedorActualizadoObj.getNombre(), id, empresaId);

        return proveedorActualizadoObj;
    }

}