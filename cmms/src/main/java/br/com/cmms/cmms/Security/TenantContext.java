package br.com.cmms.cmms.Security;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_EMPRESA_ID = new ThreadLocal<>();

    public static void setEmpresaId(Long id) { CURRENT_EMPRESA_ID.set(id); }
    public static Long getEmpresaId() { return CURRENT_EMPRESA_ID.get(); }
    public static void clear() { CURRENT_EMPRESA_ID.remove(); }
}
