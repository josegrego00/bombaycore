package jpd.sistemafacinv.sistemadefacturacioneinventario.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);
    
    @Autowired
    private CierreDiarioInterceptor cierreInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("⚙️ Configurando interceptores de Spring MVC");
        log.debug("Registrando CierreDiarioInterceptor");
        
        registry.addInterceptor(cierreInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/cierres/**", "/login", "/css/**", "/js/**", "/error");
        
        log.debug("CierreDiarioInterceptor configurado. Patrones incluidos: /**");
        log.debug("Patrones excluidos: /cierres/**, /login, /css/**, /js/**, /error");
    }
}