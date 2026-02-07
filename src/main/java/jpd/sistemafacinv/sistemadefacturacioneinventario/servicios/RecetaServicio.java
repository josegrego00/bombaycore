package jpd.sistemafacinv.sistemadefacturacioneinventario.servicios;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Ingrediente;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Receta;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.RecetaDetalle;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.RecetaRepositorio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class RecetaServicio {

    private static final Logger log = LoggerFactory.getLogger(RecetaServicio.class);

    private final RecetaRepositorio repositorio;
    private final IngredienteServicio ingredienteServicio;

    public Receta crearReceta(Receta receta) {
        Long empresaId = TenantContext.getCurrentTenant();

        // CREAR
        if (receta.getId() == null) {
            if (repositorio.existsByNombreAndEmpresa_id(receta.getNombre(), empresaId)) {
                log.warn("Intento de crear receta duplicada: {}", receta.getNombre());
                throw new RuntimeException("Ya existe una receta con ese nombre");
            }
        }
        // EDITAR
        else {
            if (repositorio.existsByNombreAndEmpresa_IdAndIdNot(receta.getNombre(), empresaId, receta.getId())) {
                log.warn("Intento de editar receta a un nombre duplicado: {}", receta.getNombre());
                throw new RuntimeException("Ya existe otra receta con ese nombre");
            }
        }
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        receta.setEmpresa(empresa);

        Receta recetaGuardada = repositorio.save(receta);
        log.info("Receta guardada exitosamente: '{}' (ID: {})", recetaGuardada.getNombre(), recetaGuardada.getId());
        return recetaGuardada;
    }

    public List<Receta> listaRecetas() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando recetas para empresa ID: {}", empresaId);

        List<Receta> recetas = repositorio.findByEmpresaIdWithIngredientes(empresaId);
        log.debug("Encontradas {} recetas para empresa ID: {}", recetas.size(), empresaId);

        return recetas;
    }

    public Receta buscarReceta(long id) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando receta ID: {} para empresa ID: {}", id, empresaId);

        Receta receta = repositorio.findByIdWithIngredientes(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Receta no encontrada ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Receta no encontrada");
                });

        log.debug("Receta encontrada: '{}' (ID: {})", receta.getNombre(), id);
        return receta;
    }

    public Receta actualizarReceta(long id, String nombre, String descripcion,
            List<Integer> ingredienteIds, List<Double> cantidades) {

        Long empresaId = TenantContext.getCurrentTenant(); // <--- AQUÍ
        log.debug("Actualizando receta ID: {}", id);

        Receta receta = buscarReceta(id);

        // Validación de nombre duplicado
        if (repositorio.existsByNombreAndEmpresa_IdAndIdNot(nombre, empresaId, receta.getId())) {
            log.warn("Intento de editar receta a un nombre duplicado: {}", nombre);
            throw new RuntimeException("Ya existe otra receta con ese nombre");
        }

        receta.setNombre(nombre);
        receta.setDescripcion(descripcion);

        // Limpiar detalles existentes para evitar duplicados
        receta.getIngredientes().clear();
        repositorio.flush();

        // Agregar nuevos detalles
        if (ingredienteIds != null && cantidades != null) {
            for (int i = 0; i < ingredienteIds.size(); i++) {
                if (ingredienteIds.get(i) != null && cantidades.get(i) > 0) {
                    Ingrediente ingrediente = ingredienteServicio.buscarIngrediente(ingredienteIds.get(i).longValue());
                    RecetaDetalle detalle = RecetaDetalle.builder()
                            .receta(receta)
                            .ingrediente(ingrediente)
                            .cantidadIngrediente(cantidades.get(i))
                            .build();
                    receta.getIngredientes().add(detalle);
                }
            }
        }

        calcularCostoReceta(receta);

        Receta recetaActualizada = repositorio.save(receta);
        log.info("Receta actualizada exitosamente: '{}' (ID: {}) con {} ingredientes",
                recetaActualizada.getNombre(), id, recetaActualizada.getIngredientes().size());

        return recetaActualizada;
    }

    public void eliminarReceta(long id) {
        log.debug("Eliminando receta ID: {}", id);

        Receta receta = buscarReceta(id);
        repositorio.delete(receta);

        log.info("Receta eliminada: '{}' (ID: {})", receta.getNombre(), id);
    }

    private void calcularCostoReceta(Receta receta) {
        log.debug("Calculando costo para receta: '{}' (ID: {})",
                receta.getNombre(), receta.getId());

        double costo = receta.getIngredientes().stream()
                .mapToDouble(detalle -> detalle.getIngrediente().getPrecio() * detalle.getCantidadIngrediente())
                .sum();

        receta.setCostoReceta(costo);
        log.debug("Costo calculado para receta '{}': {}", receta.getNombre(), costo);
    }

    public void agregarDetalle(long idReceta, RecetaDetalle detalle) {
        log.debug("Agregando detalle a receta ID: {}", idReceta);

        Receta receta = buscarReceta(idReceta);

        Long empresaIdDetalle = detalle.getIngrediente().getEmpresa().getId();
        Long empresaIdReceta = receta.getEmpresa().getId();

        if (!empresaIdDetalle.equals(empresaIdReceta)) {
            log.error(
                    "Intento de agregar ingrediente de empresa diferente. Receta empresa ID: {}, Ingrediente empresa ID: {}",
                    empresaIdReceta, empresaIdDetalle);
            throw new RuntimeException("Ingrediente no pertenece a la empresa");
        }

        detalle.setReceta(receta);
        receta.getIngredientes().add(detalle);
        calcularCostoReceta(receta);
        repositorio.save(receta);

        log.info("Detalle agregado a receta '{}' (ID: {}). Ingrediente: '{}' (ID: {}), Cantidad: {}",
                receta.getNombre(), idReceta,
                detalle.getIngrediente().getNombre(),
                detalle.getIngrediente().getId(),
                detalle.getCantidadIngrediente());
    }

    public void quitarDetalle(long idReceta, long idDetalle) {
        log.debug("Quitando detalle ID: {} de receta ID: {}", idDetalle, idReceta);

        Receta receta = buscarReceta(idReceta);
        boolean removed = receta.getIngredientes().removeIf(detalle -> detalle.getId() == idDetalle);

        if (removed) {
            calcularCostoReceta(receta);
            repositorio.save(receta);
            log.info("Detalle ID: {} quitado de receta '{}' (ID: {})",
                    idDetalle, receta.getNombre(), idReceta);
        } else {
            log.warn("Detalle ID: {} no encontrado en receta ID: {}", idDetalle, idReceta);
        }
    }

    public void actualizarCantidadDetalle(long idReceta, long idDetalle, double nuevaCantidad) {
        log.debug("Actualizando cantidad del detalle ID: {} en receta ID: {} a {}",
                idDetalle, idReceta, nuevaCantidad);

        Receta receta = buscarReceta(idReceta);

        boolean actualizado = receta.getIngredientes().stream()
                .filter(detalle -> detalle.getId() == idDetalle)
                .findFirst()
                .map(detalle -> {
                    double cantidadAnterior = detalle.getCantidadIngrediente();
                    detalle.setCantidadIngrediente(nuevaCantidad);
                    log.trace("Cantidad actualizada de {} a {} para detalle ID: {}",
                            cantidadAnterior, nuevaCantidad, idDetalle);
                    return detalle;
                })
                .isPresent();

        if (actualizado) {
            calcularCostoReceta(receta);
            repositorio.save(receta);
            log.info("Cantidad actualizada para detalle ID: {} en receta '{}' (ID: {})",
                    idDetalle, receta.getNombre(), idReceta);
        } else {
            log.warn("No se encontró detalle ID: {} en receta ID: {} para actualizar cantidad",
                    idDetalle, idReceta);
        }
    }
}