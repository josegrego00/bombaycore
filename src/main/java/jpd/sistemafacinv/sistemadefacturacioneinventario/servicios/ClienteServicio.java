package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Cliente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.ClienteRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class ClienteServicio {

    private static final Logger log = LoggerFactory.getLogger(ClienteServicio.class);
    
    private final ClienteRepositorio clienteRepo;
    private final EmpresaRepositorio empresaRepo;

    public Cliente crearCliente(Cliente cliente) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Creando cliente '{}' para empresa ID: {}", cliente.getNombre(), empresaId);

        // Validar documento único EN ESTA EMPRESA
        if (cliente.getIdentificacion() != null) {
            log.debug("Validando identificación única: {} para empresa ID: {}", 
                     cliente.getIdentificacion(), empresaId);
            
            Optional<Cliente> existente = clienteRepo
                    .findByIdentificacionAndEmpresaId(cliente.getIdentificacion(), empresaId);
            if (existente.isPresent()) {
                log.warn("Identificación duplicada: {} para empresa ID: {}", 
                         cliente.getIdentificacion(), empresaId);
                throw new RuntimeException("Ya existe un cliente con este documento en la empresa");
            }
        }
        
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> {
                    log.error("Empresa no encontrada ID: {}", empresaId);
                    return new RuntimeException("Empresa no encontrada");
                });
        
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);
        
        Cliente clienteCreado = clienteRepo.save(cliente);
        log.info("Cliente creado exitosamente: '{}' (ID: {}) para empresa ID: {}", 
                clienteCreado.getNombre(), clienteCreado.getId(), empresaId);
        
        return clienteCreado;
    }

    public List<Cliente> listarClientes() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando clientes para empresa ID: {}", empresaId);
        
        List<Cliente> clientes = clienteRepo.findByEmpresaId(empresaId);
        log.debug("Encontrados {} clientes para empresa ID: {}", clientes.size(), empresaId);
        
        return clientes;
    }

    public Cliente buscarCliente(Long id) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando cliente ID: {} para empresa ID: {}", id, empresaId);
        
        return clienteRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Cliente no encontrado ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Cliente no encontrado");
                });
    }

    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Actualizando cliente ID: {} para empresa ID: {}", id, empresaId);

        Cliente cliente = buscarCliente(id);
        log.debug("Cliente encontrado para actualizar: '{}' (ID: {})", cliente.getNombre(), id);

        // Validar documento único si cambió
        if (clienteActualizado.getIdentificacion() != null &&
                !clienteActualizado.getIdentificacion().equals(cliente.getIdentificacion())) {

            log.debug("Validando nueva identificación: {} para empresa ID: {}", 
                     clienteActualizado.getIdentificacion(), empresaId);
            
            Optional<Cliente> existente = clienteRepo
                    .findByIdentificacionAndEmpresaId(clienteActualizado.getIdentificacion(), empresaId);
            if (existente.isPresent()) {
                log.warn("Identificación duplicada: {} para empresa ID: {}", 
                         clienteActualizado.getIdentificacion(), empresaId);
                throw new RuntimeException("Ya existe otro cliente con este documento en la empresa");
            }
        }

        cliente.setNombre(clienteActualizado.getNombre());
        cliente.setTelefono(clienteActualizado.getTelefono());
        cliente.setCorreo(clienteActualizado.getCorreo());
        cliente.setIdentificacion(clienteActualizado.getIdentificacion());
        cliente.setActivo(clienteActualizado.isActivo());

        Cliente clienteActualizadoObj = clienteRepo.save(cliente);
        log.info("Cliente actualizado exitosamente: '{}' (ID: {}) para empresa ID: {}", 
                clienteActualizadoObj.getNombre(), id, empresaId);
        
        return clienteActualizadoObj;
    }

    public void desactivarCliente(Long id) {
        log.debug("Desactivando cliente ID: {}", id);
        
        Cliente cliente = buscarCliente(id);
        cliente.setActivo(false);
        clienteRepo.save(cliente);
        
        log.info("Cliente desactivado: '{}' (ID: {})", cliente.getNombre(), id);
    }

    // Métodos adicionales útiles
    public List<Cliente> buscarPorNombre(String nombre) {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando clientes por nombre: '{}' para empresa ID: {}", nombre, empresaId);
        
        List<Cliente> clientes = clienteRepo.findByNombreContainingIgnoreCaseAndEmpresaId(nombre, empresaId);
        log.debug("Encontrados {} clientes por nombre '{}' para empresa ID: {}", 
                 clientes.size(), nombre, empresaId);
        
        return clientes;
    }

    public List<Cliente> listarClientesActivos() {
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando clientes activos para empresa ID: {}", empresaId);
        
        List<Cliente> clientes = clienteRepo.findByActivoTrueAndEmpresaId(empresaId);
        log.debug("Encontrados {} clientes activos para empresa ID: {}", clientes.size(), empresaId);
        
        return clientes;
    }
}