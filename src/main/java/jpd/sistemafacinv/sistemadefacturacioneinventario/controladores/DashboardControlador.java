package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Empresa;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.EmpresaRepositorio;
import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
@RequestMapping("/principal")
public class DashboardControlador {

    private static final Logger log = LoggerFactory.getLogger(DashboardControlador.class);

    private final CierreInventarioDiarioRepositorio cierreRepository;
    private final EmpresaRepositorio empresaRepositorio;

    @GetMapping
    public String inicio(Model model, Authentication authentication) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🏠 GET /principal - Accediendo al dashboard principal");
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("username", auth.getName());
        }
        Long empresaId = TenantContext.getCurrentTenant();
        log.debug("🔍 Empresa ID actual: {}", empresaId);

        if (empresaId != null) {
            try {
                // Buscar la empresa en la base de datos
                Optional<Empresa> empresaOpt = empresaRepositorio.findById(empresaId);

                if (empresaOpt.isPresent()) {
                    Empresa empresa = empresaOpt.get();

                    // Pasar datos de la empresa al modelo
                    model.addAttribute("nombreEmpresa", empresa.getNombre());
                    model.addAttribute("subdominioEmpresa", empresa.getSubdominio());

                    log.debug("🏢 Datos empresa cargados: {} ({})",
                            empresa.getNombre(), empresa.getSubdominio());
                } else {
                    log.warn("⚠️ Empresa con ID {} no encontrada en BD", empresaId);
                    model.addAttribute("nombreEmpresa", "Sistema Restaurante");
                }
            } catch (Exception e) {
                log.error("❌ Error al cargar datos de empresa: {}", e.getMessage());
                model.addAttribute("nombreEmpresa", "Sistema Restaurante");
            }
        } else {
            log.warn("⚠️ TenantContext no tiene empresa ID");
            model.addAttribute("nombreEmpresa", "Sistema Restaurante");
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