package com.ph.backend.config.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        log.debug("[TENANT CONTEXT] 🏢 Estableciendo Tenant ID: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getCurrentTenant() {
        Long tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : 1L; // Default fallback to Copropiedad ID 1
    }

    public static void clear() {
        log.debug("[TENANT CONTEXT] 🧹 Limpiando Tenant Context");
        CURRENT_TENANT.remove();
    }
}
