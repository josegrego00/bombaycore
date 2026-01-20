package jpd.sistemafacinv.sistemadefacturacioneinventario.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.UsuarioServicio;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro que intercepta cada petición HTTP para establecer el tenant (empresa).
 * Se ejecuta ANTES de que llegue a los controllers.
 */
@Component
@AllArgsConstructor
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private final UsuarioServicio usuarioServicio;
    private final EmpresaRepositorio empresaRepositorio;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestURI = httpRequest.getRequestURI();
        if (requestURI.startsWith("/superadmin/")) {
            // Skip tenant lookup for superadmin
            log.debug("📌 SKIPPING tenant lookup for SUPER_ADMIN path: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        log.debug("📌 TenantFilter - RUTA: {}, Método: {}",
                httpRequest.getRequestURI(), httpRequest.getMethod());

        String serverName = httpRequest.getServerName(); // "centro.localhost"
        log.debug("🌐 Server Name: {}", serverName);

        String subdominio = extraerSubdominio(serverName);
        log.debug("🔍 Subdominio extraído: {}", subdominio);

        Object statusCode = httpRequest.getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            log.debug("📌 TenantFilter - STATUS: {}", statusCode);
        }

        Optional<Empresa> empresaOpt = empresaRepositorio.findBySubdominio(subdominio);

        if (empresaOpt.isPresent()) {
            // CASO 1: Subdominio encontrado (centro.localhost)
            Long empresaId = empresaOpt.get().getId();
            TenantContext.setCurrentTenant(empresaId);
            log.info("✅ Empresa establecida desde subdominio: {} (Subdominio: {})", empresaId, subdominio);

        } else if (subdominio.equals("localhost")) {
            // CASO 2: localhost sin subdominio (para desarrollo)
            // Usar primera empresa o empresa por defecto
            Optional<Empresa> primeraEmpresa = empresaRepositorio.findById(1L);
            primeraEmpresa.ifPresent(emp -> {
                TenantContext.setCurrentTenant(emp.getId());
                log.info("⚠️ Usando empresa por defecto (localhost): {} - {}", emp.getId(), emp.getNombre());
            });
        } else {
            log.warn("⚠️ Subdominio no encontrado: {}", subdominio);
        }

        try {
            // 1. Obtener autenticación actual
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 2. Si hay usuario autenticado, obtener su empresa
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                Object principal = authentication.getPrincipal();

                if (principal instanceof UserDetails) {
                    String username = ((UserDetails) principal).getUsername();
                    log.debug("👤 Usuario autenticado en TenantFilter: {}", username);

                    // 3. Buscar usuario en BD para obtener empresa_id
                    Usuario usuario = usuarioServicio.buscarPorNombreUsuario(username);

                    if (usuario != null) {
                        // SUPER_ADMIN no tiene empresa, solo usuarios normales sí
                        if ("SUPER_ADMIN".equals(usuario.getRol())) {
                            log.debug("👑 Usuario SUPER_ADMIN detectado");
                        } else if (usuario.getEmpresa() != null) {
                            // Usuario normal: validar que está en la empresa correcta
                            Long empresaUsuario = usuario.getEmpresa().getId();
                            Long empresaSubdominio = TenantContext.getCurrentTenant();

                            if (empresaUsuario.equals(empresaSubdominio)) {
                                log.debug("✅ Usuario {} validado para empresa {}", username, empresaUsuario);
                            } else {
                                log.error("🚨 ERROR: Usuario en empresa incorrecta");
                            }
                        } else {
                            log.warn("⚠️ Usuario sin empresa asignada (no es SUPER_ADMIN)");
                        }
                    }
                }
            }

            // 5. Continuar con la petición
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("❌ Error en TenantFilter: {}", e.getMessage(), e);
            throw new ServletException("Error en TenantFilter", e);
        }
    }

    public static String extraerSubdominio(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            log.warn("ServerName es null o vacío, usando 'localhost'");
            return "localhost";
        }
        String[] partes = serverName.split("\\.");
        String subdominio = partes[0];
        log.trace("ServerName: {}, Subdominio extraído: {}", serverName, subdominio);
        return subdominio;
    }
}