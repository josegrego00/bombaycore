package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.IngredienteRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class IngredienteServicio {

    private static final Logger log = LoggerFactory.getLogger(IngredienteServicio.class);

    private final IngredienteRepositorio repositorio;

    public Ingrediente crearIngrediente(Ingrediente ingrediente) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Guardando ingrediente '{}' para empresa ID: {}", ingrediente.getNombre(), empresaId);

        // CREAR
        if (ingrediente.getId() == null) {
            if (repositorio.existsByNombreAndEmpresa_Id(ingrediente.getNombre(), empresaId)) {
                log.warn("Intento de crear ingrediente duplicado: {}", ingrediente.getNombre());
                throw new RuntimeException("Ya existe un ingrediente con ese nombre");
            }
        }
        // EDITAR
        else {
            if (repositorio.existsByNombreAndEmpresa_IdAndIdNot(
                    ingrediente.getNombre(), empresaId, ingrediente.getId())) {
                log.warn("Intento de editar ingrediente a un nombre duplicado: {}", ingrediente.getNombre());
                throw new RuntimeException("Ya existe otro ingrediente con ese nombre");
            }
        }

        // Asignar empresa si no está
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        ingrediente.setEmpresa(empresa);

        Ingrediente ingredienteGuardado = repositorio.save(ingrediente);
        log.info("Ingrediente guardado exitosamente: '{}' (ID: {})", ingredienteGuardado.getNombre(),
                ingredienteGuardado.getId());
        return ingredienteGuardado;
    }

    public List<Ingrediente> listaIngredientes() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando ingredientes para empresa ID: {}", empresaId);

        List<Ingrediente> ingredientes = repositorio.findByEmpresaId(empresaId);
        log.debug("Encontrados {} ingredientes para empresa ID: {}", ingredientes.size(), empresaId);

        return ingredientes;
    }

    public Ingrediente buscarIngrediente(long id) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando ingrediente ID: {} para empresa ID: {}", id, empresaId);

        Ingrediente ingrediente = repositorio.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Ingrediente no encontrado ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Ingrediente no encontrado o no pertenece a la empresa");
                });

        log.debug("Ingrediente encontrado: '{}' (ID: {})", ingrediente.getNombre(), id);
        return ingrediente;
    }

    public Ingrediente actualizaIngrediente(long id, Ingrediente ingredienteActualizado) {
        log.debug("Actualizando ingrediente ID: {}", id);

        Ingrediente ingrediente = buscarIngrediente(id);

        ingrediente.setActivo(ingredienteActualizado.isActivo());
        ingrediente.setNombre(ingredienteActualizado.getNombre());
        ingrediente.setPrecio(ingredienteActualizado.getPrecio());
        ingrediente.setStockActual(ingredienteActualizado.getStockActual());
        ingrediente.setStockMinimo(ingredienteActualizado.getStockMinimo());
        ingrediente.setUnidadMedida(ingredienteActualizado.getUnidadMedida());

        Ingrediente ingredienteActualizadoObj = repositorio.save(ingrediente);
        log.info("Ingrediente actualizado exitosamente: '{}' (ID: {})",
                ingredienteActualizadoObj.getNombre(), id);

        return ingredienteActualizadoObj;
    }

    public void eliminarIngrediente(long id) {
        log.debug("Eliminando ingrediente ID: {}", id);

        Ingrediente ingrediente = buscarIngrediente(id);
        repositorio.delete(ingrediente);

        log.info("Ingrediente eliminado: '{}' (ID: {})", ingrediente.getNombre(), id);
    }

    public void actualizarStock(long idIngrediente, double cantidad) {
        log.debug("Actualizando stock para ingrediente ID: {}, Cantidad: {}", idIngrediente, cantidad);

        Ingrediente ing = buscarIngrediente(idIngrediente);
        double stockAnterior = ing.getStockActual();
        double nuevoStock = stockAnterior + cantidad;

        ing.setStockActual(nuevoStock);
        repositorio.save(ing);

        log.info("Stock actualizado para ingrediente '{}' (ID: {}). Anterior: {}, Nuevo: {}, Cambio: {}",
                ing.getNombre(), idIngrediente, stockAnterior, nuevoStock, cantidad);
    }

    public void aumentarStock(long idIngrediente, double cantidad) {
        log.debug("Aumentando stock para ingrediente ID: {}, Cantidad: {}", idIngrediente, cantidad);

        Ingrediente ingrediente = buscarIngrediente(idIngrediente);
        double stockAnterior = ingrediente.getStockActual();

        ingrediente.setStockActual(stockAnterior + cantidad);
        repositorio.save(ingrediente);

        log.info("Stock aumentado para ingrediente '{}' (ID: {}). Anterior: {}, Nuevo: {}, Aumento: {}",
                ingrediente.getNombre(), idIngrediente, stockAnterior, ingrediente.getStockActual(), cantidad);
    }
}