package com.ph.backend.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/auth/**").permitAll()
                        .requestMatchers("/api/v1/health/**", "/health/**").permitAll()
                        .requestMatchers("/api/v1/votaciones/login-copropietario", "/votaciones/login-copropietario").permitAll()
                        .requestMatchers("/ws-ph/**", "/ws-ph-raw/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Copropiedades (Tenants): SuperAdmin crea/asigna; autenticados pueden consultar
                        .requestMatchers(HttpMethod.POST, "/api/v1/copropiedades/**", "/copropiedades/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/copropiedades/**", "/copropiedades/**").authenticated()

                        // Gestores de Registro: Exclusivo ADMIN
                        .requestMatchers("/api/v1/gestores/**", "/gestores/**").hasRole("ADMIN")

                        // Asambleas, Unidades, Preguntas, Votaciones: Exclusivo ADMIN, GESTOR, COPROPIETARIO y APODERADO
                        .requestMatchers("/api/v1/asambleas/**", "/asambleas/**").hasAnyRole("ADMIN", "GESTOR", "COPROPIETARIO", "APODERADO")
                        .requestMatchers("/api/v1/unidades/**", "/unidades/**").hasAnyRole("ADMIN", "GESTOR", "COPROPIETARIO", "APODERADO")
                        .requestMatchers("/api/v1/preguntas/**", "/preguntas/**").hasAnyRole("ADMIN", "GESTOR", "COPROPIETARIO", "APODERADO")
                        .requestMatchers("/api/v1/votaciones/**", "/votaciones/**").hasAnyRole("ADMIN", "GESTOR", "COPROPIETARIO", "APODERADO")

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
