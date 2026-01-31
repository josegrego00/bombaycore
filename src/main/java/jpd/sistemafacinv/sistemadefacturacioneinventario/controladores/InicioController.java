package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InicioController {

    private static final Logger log = LoggerFactory.getLogger(InicioController.class);

    // 1. Landing page principal (index.html) - PÚBLICA
    @GetMapping("/")
    public String mostrarLandingPage() {
        log.info("🏠 GET / - Mostrando landing page pública");
        return "index"; // Tu página con formulario de subdominios
    }

    // 2. Login multi-tenant (solo para subdominios) - PROTEGIDA por TenantFilter
    @GetMapping("/login")
    public String mostrarLogin(HttpServletRequest request) {
        String serverName = request.getServerName();

        // SOLO permitir login si viene de subdominio
        if (!serverName.contains(".") || serverName.equals("mibombay.com")) {
            log.warn("❌ Intento de acceso a /login desde dominio principal");
            return "redirect:/"; // Redirige a la landing page
        }

        log.info("🔐 GET /login - Subdominio: {}", serverName);
        return "login";
    }

    // 4. Procesar formulario de landing page - PÚBLICO
    @PostMapping("/redirect-subdomain")
    public String redirectToSubdomain(@RequestParam String subdomain) {
        log.info("🌐 POST /redirect-subdomain - Subdominio solicitado: {}", subdomain);

        // Validar que no esté vacío
        if (subdomain == null || subdomain.trim().isEmpty()) {
            log.warn("Subdominio vacío recibido");
            return "redirect:/?error=empty&field=subdomain";
        }

        // Limpiar y validar formato
        String cleanSubdomain = subdomain.trim().toLowerCase();

        // Solo permitir letras, números y guiones
        if (!cleanSubdomain.matches("[a-z0-9\\-]+")) {
            log.warn("Subdominio inválido: {}", cleanSubdomain);
            return "redirect:/?error=invalid&subdomain=" + cleanSubdomain;
        }

        // Evitar subdominios reservados
        if (cleanSubdomain.matches("(www|admin|superadmin|api|test|dev|staging)")) {
            log.warn("Subdominio reservado: {}", cleanSubdomain);
            return "redirect:/?error=reserved&subdomain=" + cleanSubdomain;
        }

        // Redirigir al login del subdominio
        String redirectUrl = "https://" + cleanSubdomain + ".mibombay.com/login";
        log.info("✅ Redirigiendo a: {}", redirectUrl);

        return "redirect:" + redirectUrl;
    }

    // 5. Dashboard - Redirige al dashboard principal
    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("📊 GET /dashboard - Redirigiendo a /principal");
        return "redirect:/principal";
    }

    // 6. Página de acceso denegado
    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        log.warn("⛔ GET /acceso-denegado - Mostrando página de acceso denegado");
        return "acceso-denegado";
    }
}