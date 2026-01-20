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
        log.debug("Creando receta '{}' para empresa ID: {}", receta.getNombre(), empresaId);
        
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        receta.setEmpresa(empresa);
        calcularCostoReceta(receta);
        
        Receta recetaCreada = repositorio.save(receta);
        log.info("Receta creada exitosamente: '{}' (ID: {}) para empresa ID: {}", 
                recetaCreada.getNombre(), recetaCreada.getId(), empresaId);
        
        // comento.... tengo q ver q rompe
        return recetaCreada;
    }

    public List<Receta> listaRecetas() {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Listando recetas para empresa ID: {}", empresaId);
        
        List<Receta> recetas = repositorio.findByEmpresaId(empresaId);
        log.debug("Encontradas {} recetas para empresa ID: {}", recetas.size(), empresaId);
        
        return recetas;
    }

    public Receta buscarReceta(long id) {
        long empresaId = TenantContext.getCurrentTenant();
        log.debug("Buscando receta ID: {} para empresa ID: {}", id, empresaId);
        
        Receta receta = repositorio.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    log.error("Receta no encontrada ID: {} para empresa ID: {}", id, empresaId);
                    return new RuntimeException("Receta no encontrada");
                });
        
        log.debug("Receta encontrada: '{}' (ID: {})", receta.getNombre(), id);
        return receta;
    }

    public Receta actualizarReceta(long id, String nombre, String descripcion,
            List<Integer> ingredienteIds, List<Double> cantidades) {
        log.debug("Actualizando receta ID: {}", id);
        
        Receta receta = buscarReceta(id);
        receta.setNombre(nombre);
        receta.setDescripcion(descripcion);
        log.debug("Receta '{}' (ID: {}) - Datos básicos actualizados", nombre, id);

        // 4. Crear NUEVA lista de detalles (no clear())
        List<RecetaDetalle> nuevosDetalles = new ArrayList<>();

        // Agregar nuevos detalles
        if (ingredienteIds != null && cantidades != null) {
            log.debug("Actualizando {} ingredientes para receta ID: {}", ingredienteIds.size(), id);
            
            for (int i = 0; i < ingredienteIds.size(); i++) {
                if (ingredienteIds.get(i) != null && cantidades.get(i) > 0) {
                    Long ingredienteId = ingredienteIds.get(i).longValue();
                    log.trace("Agregando ingrediente ID: {} con cantidad: {} a receta ID: {}", 
                             ingredienteId, cantidades.get(i), id);

                    Ingrediente ingrediente = ingredienteServicio.buscarIngrediente(ingredienteId);

                    RecetaDetalle detalle = RecetaDetalle.builder()
                            .ingrediente(ingrediente)
                            .cantidadIngrediente(cantidades.get(i))
                            .receta(receta)
                            .build();

                    nuevosDetalles.add(detalle);
                }
            }
        }
        
        receta.setIngredientes(nuevosDetalles);
        calcularCostoReceta(receta);
        
        Receta recetaActualizada = repositorio.save(receta);
        log.info("Receta actualizada exitosamente: '{}' (ID: {}) con {} ingredientes", 
                recetaActualizada.getNombre(), id, nuevosDetalles.size());
        
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
            log.error("Intento de agregar ingrediente de empresa diferente. Receta empresa ID: {}, Ingrediente empresa ID: {}", 
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