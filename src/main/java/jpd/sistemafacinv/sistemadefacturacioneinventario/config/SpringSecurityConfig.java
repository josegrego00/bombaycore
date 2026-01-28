package jpd.sistemafacinv.sistemadefacturacioneinventario.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SpringSecurityConfig.class);

        @Bean
        @Order(1) // IMPORTANTE: Se ejecuta primero
        public SecurityFilterChain superAdminFilterChain(HttpSecurity http) throws Exception {
                log.info("🛡️ Configurando SecurityFilterChain para SUPERADMIN (Orden 1)");

                http
                                // ⚠️ SOLO aplica a rutas /superadmin/**
                                .securityMatcher("/superadmin/**")

                                .authorizeHttpRequests(auth -> auth
                                                // TODAS las rutas /superadmin/** requieren rol SUPER_ADMIN
                                                .anyRequest().hasRole("SUPER_ADMIN"))
                                .formLogin(form -> form
                                                // Login SEPARADO para superadmin
                                                .loginPage("/superadmin/login")
                                                .defaultSuccessUrl("/superadmin/dashboard", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/superadmin/logout")
                                                .logoutSuccessUrl("/superadmin/login?logout")
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .accessDeniedPage("/superadmin/acceso-denegado"));

                log.info("✅ SecurityFilterChain SUPERADMIN configurado");
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                log.info("🔐 Configurando BCryptPasswordEncoder para encriptación de contraseñas");
                return new BCryptPasswordEncoder();
        }

        // Seguridad HTTP - VERSIÓN CORREGIDA
        @Bean
        @Order(2)
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                log.info("🛡️ Configurando SecurityFilterChain - Iniciando configuración de seguridad");

                http
                                .authorizeHttpRequests(auth -> auth
                                                // 1. RECURSOS PÚBLICOS
                                                .requestMatchers(
                                                                "/",
                                                                "/redirect-subdomain",
                                                                "/login",
                                                                "/acceso-denegado",
                                                                "/css/**",
                                                                "/js/**")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()

                                                // 2. PERMISOS POR ROL
                                                // VENTAS - Solo CAJERO
                                                .requestMatchers("/dashboard").authenticated()

                                                // SOLO ADMIN (NO DEV)
                                                .requestMatchers("/compras/**", "/proveedores/**").hasRole("ADMIN")

                                                // ADMIN + DEV
                                                .requestMatchers(
                                                                "/productos/**",
                                                                "/ingredientes/**",
                                                                "/recetas/**",
                                                                "/reportes/**",
                                                                "/cierres/**")
                                                .hasAnyRole("ADMIN", "DEV")

                                                // CUALQUIER OTRA RUTA (incluye /facturas/**)
                                                .anyRequest().authenticated()) // ← Aquí entran /facturas
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/principal", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .accessDeniedPage("/acceso-denegado"));

                log.info("✅ SecurityFilterChain configurado exitosamente");
                log.debug("Configuración de seguridad aplicada:");
                log.debug("- Páginas públicas: /, /login, /acceso-denegado, /css/**, /js/**, /error");
                log.debug("- Solo ADMIN: /compras/**, /proveedores/**");
                log.debug("- ADMIN o DEV: /productos/**, /ingredientes/**, /recetas/**, /reportes/**, /cierres/**");
                log.debug("- Login: /login, Logout: /logout");
                log.debug("- Página acceso denegado: /acceso-denegado");

                return http.build();
        }
}