package com.ph.backend.config.security;

import com.ph.backend.config.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extraer Tenant ID de la cabecera HTTP X-Tenant-ID si fue enviada por el cliente
            String tenantHeader = request.getHeader("X-Tenant-ID");
            if (StringUtils.hasText(tenantHeader)) {
                try {
                    Long tenantId = Long.parseLong(tenantHeader.trim());
                    TenantContext.setCurrentTenant(tenantId);
                    log.debug("[BACKEND TENANT FILTER] 🏢 Tenant ID desde cabecera X-Tenant-ID: {}", tenantId);
                } catch (NumberFormatException e) {
                    log.warn("[BACKEND TENANT FILTER] ⚠️ Cabecera X-Tenant-ID no válida: {}", tenantHeader);
                }
            }

            String jwt = getJwtFromRequest(request);
            String uri = request.getRequestURI();

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJWT(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (StringUtils.hasText(jwt)) {
                log.warn("[BACKEND JWT FILTER] ⚠️ Request HTTP {} {} - Token JWT no válido", request.getMethod(), uri);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("[BACKEND JWT FILTER] ❌ Error en filtro de autenticación: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
