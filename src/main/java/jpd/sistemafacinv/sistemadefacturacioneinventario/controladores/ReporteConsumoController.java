package jpd.sistemafacinv.sistemadefacturacioneinventario.controladores;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO.ReporteConsumoDTO;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.ReporteConsumoServicio;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/reportes")
@AllArgsConstructor
public class ReporteConsumoController {

    private static final Logger log = LoggerFactory.getLogger(ReporteConsumoController.class);
    
    private final ReporteConsumoServicio reporteConsumoServicio;

    @GetMapping("/consumo")
    public String mostrarFormularioReporte(
            @RequestParam(value = "inicio", required = false) String inicioStr,
            @RequestParam(value = "fin", required = false) String finStr,
            Model model) {

        log.info("📊 GET /reportes/consumo - Generando reporte. Fechas inicio: {}, fin: {}", inicioStr, finStr);

        // Fechas por defecto (últimos 7 días)
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.minusDays(7);

        // Si vienen parámetros, parsearlos
        if (inicioStr != null && !inicioStr.isEmpty()) {
            inicio = LocalDate.parse(inicioStr);
            log.debug("Fecha inicio parseada: {}", inicio);
        }
        if (finStr != null && !finStr.isEmpty()) {
            fin = LocalDate.parse(finStr);
            log.debug("Fecha fin parseada: {}", fin);
        }

        log.debug("Fechas para reporte: {} a {}", inicio, fin);

        try {
            ReporteConsumoDTO reporte = reporteConsumoServicio.generarReporte(inicio, fin);
            log.info("✅ Reporte generado exitosamente. Items: {}", reporte != null ? reporte.getItems().size() : 0);

            // Agregar al modelo
            model.addAttribute("reporte", reporte);
            model.addAttribute("fechaInicio", inicio);
            model.addAttribute("fechaFin", fin);
            model.addAttribute("hoy", LocalDate.now());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            model.addAttribute("fechaInicioStr", inicio.format(formatter));
            model.addAttribute("fechaFinStr", fin.format(formatter));
            model.addAttribute("hoyStr", LocalDate.now().format(formatter));

            log.debug("Reporte añadido al modelo. Total diferencia: {}", 
                     reporte != null ? reporte.getTotalValorDiferencia() : 0);

        } catch (Exception e) {
            log.error("❌ ERROR en generarReporte: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar reporte: " + e.getMessage());
        }

        return "reportes/consumo";
    }
}