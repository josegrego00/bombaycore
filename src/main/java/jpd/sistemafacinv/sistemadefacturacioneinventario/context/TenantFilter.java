package jpd.sistemafacinv.sistemadefacturacioneinventario.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.UsuarioServicio;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.Enumeration;
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
        String serverName = httpRequest.getServerName();

        log.info("🔍 DEBUG TenantFilter - serverName: {}, Host header: {}",
                serverName, httpRequest.getHeader("Host"));

        // Listar todos los headers para debug
        Enumeration<String> headers = httpRequest.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            log.debug("   {}: {}", header, httpRequest.getHeader(header));
        }
        if (serverName.equals("mibombay.com") ||
                serverName.equals("www.mibombay.com")) {
            log.debug("🌐 Dominio PRINCIPAL detectado - Mostrando landing page: {}", serverName);
            // Solo redirigir a login si explícitamente piden /login de un subdominio válido
            // (esto se manejará después del registro)
            chain.doFilter(request, response);
            return;
        }

        // Después del if del dominio principal, pero ANTES de if
        // (requestURI.startsWith("/superadmin/"))
        log.info("🔍 DEBUG - Después de validar dominio principal");
        log.info("🔍 DEBUG - serverName: '{}', requestURI: '{}'", serverName, requestURI);

        // Luego la lógica continúa...
        if (requestURI.startsWith("/superadmin/")) {
            // Skip tenant lookup for superadmin
            log.debug("📌 SKIPPING tenant lookup for SUPER_ADMIN path: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        log.info("🔍 DEBUG - Punto CRÍTICO 1 - serverName: '{}', URI: '{}'", serverName, requestURI);

        log.debug("📌 TenantFilter - RUTA: {}, Método: {}",
                httpRequest.getRequestURI(), httpRequest.getMethod());

        log.debug("🌐 Server Name: {}", serverName);

        String subdominio = extraerSubdominio(serverName);
        log.debug("🔍 Subdominio extraído: {}", subdominio);
        // ⚠️ SI ES DOMINIO BASE (mibombay.com) → Landing page

        // ⬇️ ⬇️ ⬇️ AGREGA ESTO ⬇️ ⬇️ ⬇️
        log.info("🔍 DEBUG CRÍTICO - Subdominio: '{}'", subdominio);

        if (subdominio == null) {
            log.warn("🔍 DEBUG CRÍTICO - Subdominio es NULL! serverName: '{}'", serverName);
            log.warn("🔍 DEBUG CRÍTICO - ¿serverName.endsWith('.mibombay.com')? {}",
                    serverName.endsWith(".mibombay.com"));
            chain.doFilter(request, response);
            return;
        }
        // ⬆️ ⬆️ ⬆️ HASTA AQUÍ ⬆️ ⬆️ ⬆️

        if (subdominio == null) {
            log.debug("🌐 Sin subdominio - Mostrando landing page");
            chain.doFilter(request, response);
            return;
        }

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

    // modificado para ver q pasa...

    public static String extraerSubdominio(String serverName) {
        log.info("🔍 DEBUG extraerSubdominio INICIO - serverName: '{}'", serverName);

        if (serverName == null || serverName.isEmpty()) {
            log.info("🔍 DEBUG extraerSubdominio: serverName es null o vacío");
            return null;
        }

        log.info("🔍 DEBUG extraerSubdominio: ¿endsWith '.mibombay.com'? {}",
                serverName.endsWith(".mibombay.com"));

        // ⚠️ SI ES UNA IP → NO es subdominio
        if (serverName.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            log.debug("🌐 Es una IP, no subdominio");
            return null;
        }

        // ⚠️ SI ES EL DOMINIO BASE (mibombay.com) → NO es subdominio
        if (serverName.equals("mibombay.com") ||
                serverName.equals("www.mibombay.com")) {
            log.debug("🌐 Es el dominio BASE");
            return null;
        }

        // ⚠️ SI ES localhost → usar empresa por defecto
        if (serverName.equals("localhost") || serverName.equals("127.0.0.1")) {
            log.debug("🏠 Es localhost");
            return "localhost";
        }

        // Solo extraer si tiene formato: subdominio.mibombay.com
        if (serverName.endsWith(".mibombay.com")) {
            String subdominio = serverName.replace(".mibombay.com", "");
            log.info("🔍 DEBUG extraerSubdominio: Reemplazado '{}' -> '{}'",
                    serverName, subdominio);

            // Evitar extraer "www" o vacío
            if (!subdominio.isEmpty() && !subdominio.equals("www")) {
                log.info("🔍 DEBUG extraerSubdominio: ✅ VÁLIDO: '{}'", subdominio);
                return subdominio;
            } else {
                log.info("🔍 DEBUG extraerSubdominio: ❌ INVÁLIDO (vacío o www): '{}'", subdominio);
                return null;
            }
        }

        log.info("🔍 DEBUG extraerSubdominio: ❌ NO termina con .mibombay.com");
        return null;
    } /*
       * public static String extraerSubdominio(String serverName) {
       * if (serverName == null || serverName.isEmpty()) {
       * log.warn("ServerName es null o vacío, usando 'localhost'");
       * return "localhost";
       * }
       * String[] partes = serverName.split("\\.");
       * String subdominio = partes[0];
       * log.trace("ServerName: {}, Subdominio extraído: {}", serverName, subdominio);
       * return subdominio;
       * }
       */
}