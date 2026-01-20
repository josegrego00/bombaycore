package jpd.sistemafacinv.sistemadefacturacioneinventario.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class TenantContextCleanupFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantContextCleanupFilter.class);
    
    // Usar un atributo en la request para marcar que ya se limpió
    private static final String ALREADY_CLEANED_ATTRIBUTE = 
        "TENANT_CONTEXT_ALREADY_CLEANED";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Verificar si YA se limpió para esta petición
        Boolean alreadyCleaned = (Boolean) httpRequest.getAttribute(ALREADY_CLEANED_ATTRIBUTE);
        
        if (Boolean.TRUE.equals(alreadyCleaned)) {
            // ¡YA se limpió! No hacer nada más
            log.debug("⚠️ TenantContext ya se limpió para esta petición - continuando");
            chain.doFilter(request, response);
            return;
        }
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Marcar que YA se limpió
            httpRequest.setAttribute(ALREADY_CLEANED_ATTRIBUTE, true);
            
            // Limpiar el TenantContext
            Long empresaId = TenantContext.getCurrentTenant();
            TenantContext.clear();
            
            log.debug("✅ TenantContext limpiado - Hilo: {}, Request: {}, Empresa ID anterior: {}", 
                Thread.currentThread().getId(), 
                httpRequest.getRequestURI(),
                empresaId);
        }
    }
}