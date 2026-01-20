package jpd.sistemafacinv.sistemadefacturacioneinventario.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    
    // ThreadLocal = variable independiente por cada hilo (petición HTTP)
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Establece la empresa actual para el hilo en ejecución.
     * Se llama al inicio de cada petición HTTP.
     */
    public static void setCurrentTenant(Long tenantId) {
        log.debug("🔧 TenantContext.setCurrentTenant({}) - Hilo: {}", 
                 tenantId, Thread.currentThread().getId());
        
        if (tenantId == null) {
            clear();
        } else {
            CURRENT_TENANT.set(tenantId);
            log.trace("✅ Tenant establecido: {} para hilo: {}", 
                     tenantId, Thread.currentThread().getId());
        }
    }

    /**
     * Obtiene la empresa actual del hilo en ejecución.
     * Se usa en servicios/repositorios para filtrar queries.
     */
    public static Long getCurrentTenant() {
        Long tenantId = CURRENT_TENANT.get();
        log.trace("🔍 TenantContext.getCurrentTenant() = {} - Hilo: {}", 
                 tenantId, Thread.currentThread().getId());
        return tenantId;
    }

    /**
     * Verifica si hay una empresa establecida en el contexto.
     */
    public static boolean hasTenant() {
        boolean hasTenant = CURRENT_TENANT.get() != null;
        log.trace("❓ TenantContext.hasTenant() = {} - Hilo: {}", 
                 hasTenant, Thread.currentThread().getId());
        return hasTenant;
    }

    /**
     * Limpia el contexto al final de la petición.
     * IMPORTANTE: Previene fugas de memoria.
     */
    public static void clear() {
        Long previousTenant = CURRENT_TENANT.get();
        CURRENT_TENANT.remove();
        
        if (previousTenant != null) {
            log.debug("🧹 TenantContext.clear() - Hilo: {}, Tenant anterior: {}", 
                     Thread.currentThread().getId(), previousTenant);
        } else {
            log.trace("🧹 TenantContext.clear() - Hilo: {} (sin tenant establecido)", 
                     Thread.currentThread().getId());
        }
    }

}