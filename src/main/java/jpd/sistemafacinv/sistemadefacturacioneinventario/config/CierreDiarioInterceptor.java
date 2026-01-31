package jpd.sistemafacinv.sistemadefacturacioneinventario.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jpd.sistemafacinv.sistemadefacturacioneinventario.context.TenantContext;
import jpd.sistemafacinv.sistemadefacturacioneinventario.repositorios.CierreInventarioDiarioRepositorio;

@Component
public class CierreDiarioInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CierreDiarioInterceptor.class);

    @Autowired
    private CierreInventarioDiarioRepositorio cierreRepo;

    private static final int HORA_CAMBIO_DIA = 5; // 5:00 AM

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String ruta = request.getRequestURI();
        Long empresaId = TenantContext.getCurrentTenant();

        log.debug("🛡️ Interceptor ejecutando - Ruta: {}, Empresa ID: {}", ruta, empresaId);
        if (empresaId == null) {
            log.debug("🔓 Acceso público sin empresa - Permitir: {}", ruta);
            return true;
        }

        // ⚠️ PRIMERO: Verificar si ruta es null
        if (ruta == null) {
            log.debug("🔓 Ruta null - Permitir acceso");
            return true;
        }

        // ⚠️ SEGUNDO: Rutas públicas que NUNCA deben validar cierre
        if (ruta.equals("/") ||
                ruta.equals("/redirect-subdomain") ||
                ruta.startsWith("/superadmin/")) {
            log.debug("🔓 Ruta PÚBLICA excluida del interceptor: {}", ruta);
            return true;
        }
        // En preHandle(), al inicio:
        if (ruta.equals("/acceso-denegado")) {
            log.debug("🔓 Interceptando acceso-denegado - EMPRESA: {}", empresaId);
            return true; // PERMITIR SIEMPRE
        }

        // 1. Permitir rutas NECESARIAS
        if (ruta.contains("/cierres") ||
                ruta.contains("/static") ||
                ruta.contains("/error") ||
                ruta.contains("/login") ||
                ruta.contains("/acceso-denegado")) {
            log.debug("🔓 Ruta permitida sin validación: {}", ruta);
            return true;
        }

        // 2. Obtener fecha y hora ACTUAL
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        int horaActual = ahora.getHour();

        log.debug("📅 Validación de cierre - Fecha: {}, Hora: {}:00", hoy, horaActual);

        // 3. PRIMERO: Verificar si HOY ya está cerrado (BLOQUEO INMEDIATO)
        boolean hoyCerrado = cierreRepo.existeCierrePorFechaYEstadoYEmpresaId(hoy, "COMPLETADO", empresaId);

        if (hoyCerrado) {
            log.warn("🚫 ¡HOY YA ESTÁ CERRADO! - Bloqueando acceso. Empresa ID: {}, Fecha: {}", empresaId, hoy);

            // Determinar cuándo podrá usar el sistema de nuevo
            LocalDateTime proximoAcceso = LocalDateTime.of(hoy.plusDays(1), LocalTime.of(HORA_CAMBIO_DIA, 0));

            response.sendRedirect("/cierres/bloqueado?proximoAcceso=" +
                    proximoAcceso.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            return false;
        }

        // 3. Determinar qué día validar
        LocalDate fechaParaValidar;

        if (horaActual >= HORA_CAMBIO_DIA) {
            // Si son las 5:00 AM o después → validar AYER
            fechaParaValidar = hoy.minusDays(1);
            log.debug("✅ Después de las {}:00 AM → Validar AYER: {}", HORA_CAMBIO_DIA, fechaParaValidar);
        } else {
            // Si son antes de las 5:00 AM → validar ANTEAYER
            fechaParaValidar = hoy.minusDays(2);
            log.debug("⏳ Antes de las {}:00 AM → Validar ANTEAYER: {}", HORA_CAMBIO_DIA, fechaParaValidar);
        }

        // 4. Validar cierre
        boolean diaValidadoCerrado = cierreRepo.existeCierrePorFechaYEstadoYEmpresaId(fechaParaValidar, "COMPLETADO",
                empresaId);
        log.debug("¿Día {} cerrado?: {} (Empresa ID: {})", fechaParaValidar, diaValidadoCerrado, empresaId);

        if (!diaValidadoCerrado) {
            log.warn("🚫 BLOQUEANDO acceso - Día {} no cerrado. Redirigiendo a /cierres/obligatorio", fechaParaValidar);
            response.sendRedirect("/cierres/obligatorio?fecha=" + fechaParaValidar);
            return false;
        }

        log.debug("✅ Acceso permitido - Día {} cerrado correctamente", fechaParaValidar);

        return true;
    }
}