package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/")
public class InicioController {

    private static final Logger log = LoggerFactory.getLogger(InicioController.class);

    @GetMapping
    public String redirigirALogin() {
        log.info("🔀 GET / - Redirigiendo a /login");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        log.info("🔐 GET /login - Mostrando página de login");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("📊 GET /dashboard - Redirigiendo a /principal");
        return "redirect:/principal";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        log.warn("⛔ GET /acceso-denigado - Mostrando página de acceso denegado");
        return "acceso-denegado";
    }

}