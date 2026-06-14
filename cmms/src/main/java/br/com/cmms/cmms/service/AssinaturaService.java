package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.AssinaturaInfoDTO;
import br.com.cmms.cmms.dto.CheckoutResultDTO;
import br.com.cmms.cmms.dto.PlanoInfoDTO;
import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Assinatura;
import br.com.cmms.cmms.model.Plano;
import br.com.cmms.cmms.repository.AssinaturaRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Subscription lifecycle + per-empresa plan/quota enforcement.
 *
 * <p>Payment gateway (Asaas/Stripe) is intentionally out of scope for now:
 * {@link #checkout} activates the plan manually and returns an empty payment
 * link. When the gateway lands, checkout will return a real link and the plan
 * activates only after a payment webhook.
 */
@Service
public class AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);
    private static final int BILLING_CYCLE_DAYS = 30;

    private final AssinaturaRepository assinaturaRepo;
    private final MaquinaRepository maquinaRepo;
    private final UsuarioRepository usuarioRepo;

    public AssinaturaService(AssinaturaRepository assinaturaRepo,
                             MaquinaRepository maquinaRepo,
                             UsuarioRepository usuarioRepo) {
        this.assinaturaRepo = assinaturaRepo;
        this.maquinaRepo = maquinaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    /** Static catalog of plans for the pricing grid (ordered). */
    public Map<String, PlanoInfoDTO> planos() {
        Map<String, PlanoInfoDTO> out = new LinkedHashMap<>();
        for (Plano p : Plano.values()) {
            out.put(p.name(), PlanoInfoDTO.from(p));
        }
        return out;
    }

    @Transactional
    public AssinaturaInfoDTO minha(Long empresaId) {
        return AssinaturaInfoDTO.from(ensureForEmpresa(empresaId));
    }

    @Transactional
    public CheckoutResultDTO checkout(Long empresaId, String planoNome) {
        Plano plano = parsePlano(planoNome);
        Assinatura a = ensureForEmpresa(empresaId);
        a.setPlano(plano.name());
        // No gateway yet: activate manually so the workspace is usable.
        a.setStatus(Assinatura.ATIVA);
        a.setDataProximaCobranca(LocalDate.now().plusDays(BILLING_CYCLE_DAYS));
        assinaturaRepo.save(a);
        log.info("Checkout empresa={} plano={}", empresaId, plano);
        // linkPagamento empty → frontend shows the "configure gateway" notice.
        return new CheckoutResultDTO(AssinaturaInfoDTO.from(a), "");
    }

    @Transactional
    public AssinaturaInfoDTO upgrade(Long empresaId, String planoNome) {
        Plano plano = parsePlano(planoNome);
        Assinatura a = ensureForEmpresa(empresaId);
        a.setPlano(plano.name());
        if (Assinatura.CANCELADA.equals(a.getStatus())) {
            // Re-subscribing from a cancelled state reactivates the plan.
            a.setStatus(Assinatura.ATIVA);
            a.setDataProximaCobranca(LocalDate.now().plusDays(BILLING_CYCLE_DAYS));
        }
        assinaturaRepo.save(a);
        log.info("Plano alterado empresa={} -> {}", empresaId, plano);
        return AssinaturaInfoDTO.from(a);
    }

    @Transactional
    public void cancelar(Long empresaId) {
        Assinatura a = ensureForEmpresa(empresaId);
        a.setStatus(Assinatura.CANCELADA);
        assinaturaRepo.save(a);
        log.info("Assinatura cancelada empresa={}", empresaId);
    }

    // ── enforcement ───────────────────────────────────────────────────────

    /** Throws when the empresa already hit its active-machine quota. */
    @Transactional
    public void assertPodeCriarMaquina(Long empresaId) {
        Plano plano = ensureForEmpresa(empresaId).getPlanoEnum();
        if (plano.ativosIlimitados()) return;
        long atuais = maquinaRepo.countByEmpresaId(empresaId);
        if (atuais >= plano.getLimiteAtivos()) {
            throw new ForbiddenException("QUOTA_ATIVOS",
                "Limite de " + plano.getLimiteAtivos() + " ativos do plano " + plano.name()
                    + " atingido. Faça upgrade do plano para cadastrar mais.");
        }
    }

    /** Throws when the empresa already hit its user quota. */
    @Transactional
    public void assertPodeCriarUsuario(Long empresaId) {
        Plano plano = ensureForEmpresa(empresaId).getPlanoEnum();
        if (plano.usuariosIlimitados()) return;
        long atuais = usuarioRepo.countByEmpresaId(empresaId);
        if (atuais >= plano.getLimiteUsuarios()) {
            throw new ForbiddenException("QUOTA_USUARIOS",
                "Limite de " + plano.getLimiteUsuarios() + " usuários do plano " + plano.name()
                    + " atingido. Faça upgrade do plano para convidar mais.");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Returns the empresa's subscription, lazily creating a TRIAL one. */
    @Transactional
    public Assinatura ensureForEmpresa(Long empresaId) {
        return assinaturaRepo.findByEmpresaId(empresaId)
            .orElseGet(() -> assinaturaRepo.save(new Assinatura(empresaId)));
    }

    private Plano parsePlano(String nome) {
        Plano plano = Plano.fromNome(nome);
        if (plano == null) {
            throw new ValidationException("INVALID_PLAN", "Plano inválido: " + nome);
        }
        return plano;
    }
}
