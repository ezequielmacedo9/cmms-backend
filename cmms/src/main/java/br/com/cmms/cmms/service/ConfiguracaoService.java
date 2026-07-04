package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import br.com.cmms.cmms.repository.ConfiguracaoSistemaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-tenant system settings. Every empresa gets its own copy of the default
 * catalog, seeded lazily on first read so new tenants never need a manual
 * setup step.
 */
@Service
public class ConfiguracaoService {

    private final ConfiguracaoSistemaRepository repo;
    private final TenantResolver tenant;

    public ConfiguracaoService(ConfiguracaoSistemaRepository repo, TenantResolver tenant) {
        this.repo = repo;
        this.tenant = tenant;
    }

    /** Default catalog; unknown keys are never accepted via the API. */
    private List<ConfiguracaoSistema> defaults(Long empresaId) {
        return List.of(
            cfg(empresaId, "empresa.nome",                "CMMS Industrial Suite", "GERAL",       "STRING",  "Nome da empresa"),
            cfg(empresaId, "empresa.descricao",           "Sistema de Gestão de Manutenção Industrial", "GERAL", "STRING", "Descrição"),
            cfg(empresaId, "empresa.timezone",            "America/Sao_Paulo",     "GERAL",       "STRING",  "Fuso horário"),
            cfg(empresaId, "empresa.idioma",              "pt-BR",                 "GERAL",       "STRING",  "Idioma padrão"),
            cfg(empresaId, "seguranca.lockout.tentativas","5",                     "SEGURANCA",   "NUMBER",  "Tentativas antes do bloqueio"),
            cfg(empresaId, "seguranca.lockout.minutos",   "15",                    "SEGURANCA",   "NUMBER",  "Minutos de bloqueio"),
            cfg(empresaId, "seguranca.senha.minLength",   "8",                     "SEGURANCA",   "NUMBER",  "Comprimento mínimo da senha"),
            cfg(empresaId, "seguranca.2fa.obrigatorio",   "false",                 "SEGURANCA",   "BOOLEAN", "Exigir 2FA para todos"),
            cfg(empresaId, "notificacao.email.habilitado","false",                 "NOTIFICACAO", "BOOLEAN", "Notificações por email"),
            cfg(empresaId, "notificacao.manutencao.diasAviso","7",                 "NOTIFICACAO", "NUMBER",  "Dias de aviso para preventiva")
        );
    }

    /** Idempotent: inserts only the catalog keys the empresa doesn't have yet. */
    @Transactional
    public void seedForEmpresa(Long empresaId) {
        for (ConfiguracaoSistema d : defaults(empresaId)) {
            if (!repo.existsByEmpresaIdAndChave(empresaId, d.getChave())) repo.save(d);
        }
    }

    /** Legacy non-paged listing — useful for clients that want everything in one shot. */
    @Transactional
    public List<ConfiguracaoSistema> listar() {
        Long empresaId = tenant.requireEmpresaId();
        seedForEmpresa(empresaId);
        return repo.findByEmpresaId(empresaId);
    }

    /** Paged listing with optional search and group filter. */
    @Transactional
    public Page<ConfiguracaoSistema> listar(String q, String grupo, Pageable pageable) {
        Long empresaId = tenant.requireEmpresaId();
        seedForEmpresa(empresaId);
        String normalizedQ     = (q == null     || q.isBlank())     ? null : q.trim();
        String normalizedGrupo = (grupo == null || grupo.isBlank()) ? null : grupo.trim();
        return repo.search(normalizedQ, normalizedGrupo, empresaId, pageable);
    }

    /**
     * Reads a raw value for a specific empresa. The tenant id is explicit
     * because some callers (e.g. login lockout) run before authentication.
     */
    public Optional<String> get(Long empresaId, String chave) {
        if (empresaId == null) return Optional.empty();
        return repo.findByEmpresaIdAndChave(empresaId, chave).map(ConfiguracaoSistema::getValor);
    }

    public int getInt(Long empresaId, String chave, int defaultVal) {
        return get(empresaId, chave)
            .map(v -> { try { return Integer.parseInt(v); } catch (Exception e) { return defaultVal; } })
            .orElse(defaultVal);
    }

    public boolean getBool(Long empresaId, String chave) {
        return "true".equalsIgnoreCase(get(empresaId, chave).orElse("false"));
    }

    /** Updates existing keys of the current empresa; unknown keys are ignored. */
    @Transactional
    public void salvar(Map<String, String> values) {
        Long empresaId = tenant.requireEmpresaId();
        seedForEmpresa(empresaId);
        for (Map.Entry<String, String> e : values.entrySet()) {
            repo.findByEmpresaIdAndChave(empresaId, e.getKey()).ifPresent(cfg -> {
                cfg.setValor(e.getValue());
                repo.save(cfg);
            });
        }
    }

    private static ConfiguracaoSistema cfg(Long empresaId, String k, String v, String g, String t, String d) {
        return new ConfiguracaoSistema(empresaId, k, v, g, t, d);
    }
}
