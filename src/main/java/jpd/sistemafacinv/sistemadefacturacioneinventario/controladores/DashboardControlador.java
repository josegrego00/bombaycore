package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
@RequestMapping("/principal")
public class DashboardControlador {

    private static final Logger log = LoggerFactory.getLogger(DashboardControlador.class);

    private final CierreInventarioDiarioRepositorio cierreRepository;

    @GetMapping
    public String inicio(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🏠 GET /principal - Accediendo al dashboard principal");
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("username", auth.getName());
        }

        return "dashboard/dashboard";
    }

    @GetMapping("/inicio")
    public String inicioAlternativo() {
        log.info("🏠 GET /principal/inicio - Accediendo al dashboard (ruta alternativa)");
        return "dashboard/dashboard";
    }

    @ModelAttribute
    public void agregarEstadoDia(Model model) {
        LocalDate hoy = LocalDate.now();
        log.debug("📅 Agregando estado del día: {}", hoy);

        CierreInventarioDiario cierreHoy = cierreRepository.findCierreCompletadoByFecha(hoy);

        model.addAttribute("fechaHoy", hoy);
        model.addAttribute("cierreHoy", cierreHoy);

        if (cierreHoy != null) {
            log.debug("Estado cierre hoy: {} - Total ventas: {}",
                    cierreHoy.getEstado(), cierreHoy.getTotalVentas());
        } else {
            log.debug("No hay cierre completado para hoy: {}", hoy);
        }
    }

}