package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.CierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DetalleCierreInventarioDiario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.Usuario;
import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.DetallesCierreDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.CierreInventarioDiarioService;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.UsuarioServicio;
import lombok.AllArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cierres")
@AllArgsConstructor
public class CierreInventarioDiarioControlador {

    private static final Logger log = LoggerFactory.getLogger(CierreInventarioDiarioControlador.class);

    private final CierreInventarioDiarioService cierreService;
    private final UsuarioServicio usuarioServicio;

    // 1. LISTAR CIERRES
    @GetMapping
    public String listarCierres(Model modelo) {
        log.info("📋 GET /cierres - Listando cierres diarios");
        List<CierreInventarioDiario> cierres = cierreService.obtenerTodosCierres();
        modelo.addAttribute("cierres", cierres);
        log.debug("Encontrados {} cierres", cierres.size());
        return "cierres/lista";
    }

    // 2. INICIAR NUEVO CIER
    @GetMapping("/iniciar")
    public String iniciarNuevoCierre(@RequestParam String fecha,
            Authentication authentication,
            Model model) { // ← Agregar Model

        String username = authentication.getName();
        log.info("🚀 GET /cierres/iniciar - Iniciando nuevo cierre. Usuario: {}, Fecha: {}",
                username, fecha);

        Usuario usuario = usuarioServicio.buscarPorNombreUsuario(username);

        try {
            // La fecha SIEMPRE es requerida cuando se viene del calendario
            LocalDate fechaCierre = LocalDate.parse(fecha);
            cierreService.iniciarCierreParaFecha(usuario, fechaCierre);

            log.info("✅ Cierre iniciado para fecha específica: {} por usuario: {}",
                    fecha, username);

            return "redirect:/cierres/en-proceso";

        } catch (Exception e) {
            log.error("❌ Error al iniciar cierre. Usuario: {}, Fecha: {}, Error: {}",
                    username, fecha, e.getMessage(), e);

            // Usar flash attribute en lugar de parámetro URL
            return "redirect:/cierres/obligatorio";
        }
    } // 3. VISTA DETALLE CIERRE EN PROCESO

    @GetMapping("/en-proceso")
    public String verCierreEnProceso(Model modelo) {
        log.info("🔍 GET /cierres/en-proceso - Viendo cierre en proceso");
        Optional<CierreInventarioDiario> cierreOpt = cierreService.obtenerCierreEnProceso();

        if (cierreOpt.isEmpty()) {
            log.warn("⚠️ No hay cierre en proceso. Redirigiendo a lista de cierres");
            return "redirect:/cierres";
        }

        CierreInventarioDiario cierre = cierreOpt.get();
        List<DetalleCierreInventarioDiario> detalles = cierreService.obtenerDetallesCierre(cierre.getId());

        modelo.addAttribute("cierre", cierre);
        modelo.addAttribute("detalles", detalles);

        // Calcular total diferencia (cambié de ValorDiferencia a Diferencia)
        double totalDiferencia = detalles.stream()
                .mapToDouble(DetalleCierreInventarioDiario::getDiferencia)
                .sum();
        modelo.addAttribute("totalDiferencia", totalDiferencia);

        log.debug("Cierre en proceso ID: {}, Estado: {}, Detalles: {}, Total diferencia: {}",
                cierre.getId(), cierre.getEstado(), detalles.size(), totalDiferencia);
        return "cierres/proceso";
    }

    @GetMapping("/detalle/{id}")
    public String verDetallesCierre(@PathVariable Long id, Model model) {
        log.info("👁️ GET /cierres/detalle/{} - Viendo detalles de cierre", id);
        CierreInventarioDiario cierre = cierreService.buscarCierre(id);
        List<DetalleCierreInventarioDiario> detalles = cierreService.obtenerDetallesCierre(id);

        model.addAttribute("cierre", cierre);
        model.addAttribute("detalles", detalles);

        // Calcular total diferencia
        double totalDiferencia = detalles.stream()
                .mapToDouble(DetalleCierreInventarioDiario::getDiferencia)
                .sum();
        model.addAttribute("totalDiferencia", totalDiferencia);

        log.debug("Cierre ID: {}, Estado: {}, Detalles: {}, Diferencia total: {}",
                id, cierre.getEstado(), detalles.size(), totalDiferencia);

        // Si está PRE-COMPLETADO, mostrar la misma vista para editar
        if ("PRE-COMPLETADO".equals(cierre.getEstado())) {
            log.debug("Cierre PRE-COMPLETADO, mostrando vista para editar");
            return "cierres/proceso";
        }

        return "cierres/detalle-cierre";
    }

    // 4. GUARDAR DETALLE
    @PostMapping("/detalle/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarDetalle(
            @RequestParam Long detalleId,
            @RequestParam double stockReal,
            @RequestParam double merma,
            @RequestParam double desperdicio) {

        log.info("💾 POST /cierres/detalle/guardar - Guardando detalle ID: {}", detalleId);
        log.debug("Detalle {} - Stock real: {}, Merma: {}, Desperdicio: {}",
                detalleId, stockReal, merma, desperdicio);

        cierreService.actualizarDetalle(detalleId, stockReal, merma, desperdicio);
        log.debug("Detalle {} guardado exitosamente", detalleId);

        return ResponseEntity.ok().build();
    }

    // 5. COMPLETAR CIERRE (PRE-COMPLETADO)
    @PostMapping("/completar/{id}")
    public String completarCierre(@PathVariable Long id,
            @ModelAttribute DetallesCierreDTO request) {

        log.info("📝 POST /cierres/completar/{} - Completando cierre (PRE-COMPLETADO)", id);
        log.debug("Recibidos {} detalles para guardar",
                request.getDetalles() != null ? request.getDetalles().size() : 0);

        // Guardar todos los detalles
        if (request.getDetalles() != null) {
            for (DetallesCierreDTO.DetalleRequest detalleReq : request.getDetalles()) {
                cierreService.actualizarDetalle(
                        detalleReq.getId(),
                        detalleReq.getStockReal(),
                        detalleReq.getMerma(),
                        detalleReq.getDesperdicio());
            }
            log.debug("{} detalles actualizados", request.getDetalles().size());
        }

        // Solo pasa a PRE-COMPLETADO (NO ajusta inventario)
        cierreService.completarCierre(id);
        log.info("✅ Cierre marcado como PRE-COMPLETADO ID: {}", id);

        return "redirect:/cierres";
    }

    // 6. COMPLETAR DEFINITIVO (COMPLETADO)
    @PostMapping("/completar-definitivo/{id}")
    public String completarCierreDefinitivo(@PathVariable Long id,
            @ModelAttribute DetallesCierreDTO request) {

        log.info("✅ POST /cierres/completar-definitivo/{} - Completando cierre definitivamente", id);

        // Guardar merma/desperdicio (si hay)
        if (request.getDetalles() != null) {
            log.debug("Actualizando {} detalles antes de completar", request.getDetalles().size());
            for (DetallesCierreDTO.DetalleRequest detalleReq : request.getDetalles()) {
                cierreService.actualizarDetalle(
                        detalleReq.getId(),
                        detalleReq.getStockReal(),
                        detalleReq.getMerma(),
                        detalleReq.getDesperdicio());
            }
        }

        // Pasa a COMPLETADO y ajusta inventario si se marca
        cierreService.completarCierreDefinitivo(id);
        log.info("✅ Cierre completado definitivamente ID: {}", id);

        return "redirect:/cierres";
    }

    @GetMapping("/detalleCierre/{id}")
    public String verDetallesCierreDefinitivo(@PathVariable Long id, Model model) {
        log.info("📊 GET /cierres/detalleCierre/{} - Viendo detalles completos de cierre", id);

        CierreInventarioDiario cierre = cierreService.buscarCierre(id);
        List<DetalleCierreInventarioDiario> detalles = cierreService.obtenerDetallesCierre(id);

        model.addAttribute("cierre", cierre);
        model.addAttribute("detalles", detalles);

        // Calcular totales
        double totalDiferencia = detalles.stream()
                .mapToDouble(DetalleCierreInventarioDiario::getDiferencia).sum();
        double totalValorDiferencia = detalles.stream()
                .mapToDouble(DetalleCierreInventarioDiario::getValorDiferencia).sum();

        // NUEVO: Calcular VALOR de merma y desperdicio
        double totalValorMerma = detalles.stream()
                .mapToDouble(d -> d.getStockMerma() * d.getCostoUnitario()).sum();
        double totalValorDesperdicio = detalles.stream()
                .mapToDouble(d -> d.getStockDesperdicio() * d.getCostoUnitario()).sum();

        model.addAttribute("totalDiferencia", totalDiferencia);
        model.addAttribute("totalValorDiferencia", totalValorDiferencia);
        model.addAttribute("totalValorMerma", totalValorMerma);
        model.addAttribute("totalValorDesperdicio", totalValorDesperdicio);

        log.debug("Cierre ID: {} - Diferencia: {}, Valor diferencia: {}, Valor merma: {}, Valor desperdicio: {}",
                id, totalDiferencia, totalValorDiferencia, totalValorMerma, totalValorDesperdicio);

        return "cierres/detalle-cierre";
    }

    @GetMapping("/obligatorio")
    public String cierreObligatorio(@RequestParam(required = false) String fecha,
            @RequestParam(required = false) String error,
            Model model) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        int horaActual = ahora.getHour();

        log.info("⏰ GET /cierres/obligatorio - Verificando cierre obligatorio");
        log.debug("Hora actual: {}:00, Fecha: {}", horaActual, hoy);

        // Determinar fecha pendiente
        LocalDate fechaPendiente;
        if (fecha != null) {
            fechaPendiente = LocalDate.parse(fecha);
            log.debug("Fecha proporcionada: {}", fecha);
        } else if (horaActual >= 5) {
            fechaPendiente = hoy.minusDays(1); // Después de 5 AM → ayer
            log.debug("Después de 5 AM, fecha pendiente: {} (ayer)", fechaPendiente);
        } else {
            fechaPendiente = hoy.minusDays(2); // Antes de 5 AM → anteayer
            log.debug("Antes de 5 AM, fecha pendiente: {} (anteayer)", fechaPendiente);
        }

        model.addAttribute("fechaPendiente", fechaPendiente);
        model.addAttribute("hoy", hoy);
        model.addAttribute("horaActual", String.format("%02d:00", horaActual));
        model.addAttribute("horaCambio", "05:00");
        if (error != null) {
            model.addAttribute("error", error);
            log.warn("⚠️ Error recibido: {}", error);
        }

        log.info("⚠️ Cierre obligatorio requerido para fecha: {}", fechaPendiente);
        return "cierres/obligatorio";
    }

    @GetMapping("/bloqueado")
    public String sistemaBloqueado(@RequestParam String proximoAcceso, Model model) {
        log.warn("⛔ GET /cierres/bloqueado - Sistema bloqueado. Próximo acceso: {}", proximoAcceso);
        model.addAttribute("proximoAcceso", proximoAcceso);
        return "cierres/bloqueado";
    }
}