package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import br.com.cmms.cmms.repository.ConfiguracaoSistemaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfiguracaoService {

    private final ConfiguracaoSistemaRepository repo;

    public ConfiguracaoService(ConfiguracaoSistemaRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void seed() {
        List<ConfiguracaoSistema> defaults = List.of(
            cfg("empresa.nome",                "CMMS Industrial Suite", "GERAL",       "STRING",  "Nome da empresa"),
            cfg("empresa.descricao",           "Sistema de Gestão de Manutenção Industrial", "GERAL", "STRING", "Descrição"),
            cfg("empresa.timezone",            "America/Sao_Paulo",     "GERAL",       "STRING",  "Fuso horário"),
            cfg("empresa.idioma",              "pt-BR",                 "GERAL",       "STRING",  "Idioma padrão"),
            cfg("seguranca.lockout.tentativas","5",                     "SEGURANCA",   "NUMBER",  "Tentativas antes do bloqueio"),
            cfg("seguranca.lockout.minutos",   "15",                    "SEGURANCA",   "NUMBER",  "Minutos de bloqueio"),
            cfg("seguranca.senha.minLength",   "8",                     "SEGURANCA",   "NUMBER",  "Comprimento mínimo da senha"),
            cfg("seguranca.2fa.obrigatorio",   "false",                 "SEGURANCA",   "BOOLEAN", "Exigir 2FA para todos"),
            cfg("notificacao.email.habilitado","false",                 "NOTIFICACAO", "BOOLEAN", "Notificações por email"),
            cfg("notificacao.manutencao.diasAviso","7",                 "NOTIFICACAO", "NUMBER",  "Dias de aviso para preventiva")
        );
        for (ConfiguracaoSistema d : defaults) {
            if (!repo.existsById(d.getChave())) repo.save(d);
        }
    }

    /** Legacy non-paged listing — useful for clients that want everything in one shot. */
    public List<ConfiguracaoSistema> listar() {
        return repo.findAll();
    }

    /** Paged listing with optional search and group filter. */
    public Page<ConfiguracaoSistema> listar(String q, String grupo, Pageable pageable) {
        String normalizedQ     = (q == null     || q.isBlank())     ? null : q.trim();
        String normalizedGrupo = (grupo == null || grupo.isBlank()) ? null : grupo.trim();
        return repo.search(normalizedQ, normalizedGrupo, pageable);
    }

    public Optional<String> get(String chave) {
        return repo.findById(chave).map(ConfiguracaoSistema::getValor);
    }

    public int getInt(String chave, int defaultVal) {
        return get(chave)
            .map(v -> { try { return Integer.parseInt(v); } catch (Exception e) { return defaultVal; } })
            .orElse(defaultVal);
    }

    public boolean getBool(String chave) {
        return "true".equalsIgnoreCase(get(chave).orElse("false"));
    }

    @Transactional
    public void salvar(Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            repo.findById(e.getKey()).ifPresent(cfg -> {
                cfg.setValor(e.getValue());
                repo.save(cfg);
            });
        }
    }

    private static ConfiguracaoSistema cfg(String k, String v, String g, String t, String d) {
        return new ConfiguracaoSistema(k, v, g, t, d);
    }
}
