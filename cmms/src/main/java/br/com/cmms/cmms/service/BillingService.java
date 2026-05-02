package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.model.*;
import br.com.cmms.cmms.repository.AssinaturaRepository;
import br.com.cmms.cmms.repository.EmpresaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final AssinaturaRepository assinaturaRepository;
    private final EmpresaRepository empresaRepository;
    private final AsaasService asaasService;
    private final ObjectMapper objectMapper;

    public BillingService(AssinaturaRepository assinaturaRepository,
                          EmpresaRepository empresaRepository,
                          AsaasService asaasService,
                          ObjectMapper objectMapper) {
        this.assinaturaRepository = assinaturaRepository;
        this.empresaRepository = empresaRepository;
        this.asaasService = asaasService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> listarPlanos() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (PlanoAssinatura plano : PlanoAssinatura.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("nome", plano.name());
            info.put("limiteAtivos", plano.getLimiteAtivos() == Integer.MAX_VALUE ? "Ilimitado" : plano.getLimiteAtivos());
            info.put("limiteUsuarios", plano.getLimiteUsuarios() == Integer.MAX_VALUE ? "Ilimitado" : plano.getLimiteUsuarios());
            info.put("valorMensal", asaasService.getPreco(plano));
            result.put(plano.name(), info);
        }
        return result;
    }

    public Map<String, Object> getMinhaAssinatura() {
        Long empresaId = requireEmpresaId();
        Assinatura a = assinaturaRepository.findByEmpresaId(empresaId)
            .orElseGet(() -> criarAssinaturaTrialParaEmpresa(empresaId));
        return toMap(a);
    }

    @Transactional
    public Map<String, Object> checkout(PlanoAssinatura plano) {
        Long empresaId = requireEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        Assinatura assinatura = assinaturaRepository.findByEmpresaId(empresaId)
            .orElseGet(() -> criarAssinaturaTrialParaEmpresa(empresaId));

        assinatura.setPlano(plano);
        assinatura.setValorMensal(asaasService.getPreco(plano));

        // Create/update gateway customer
        String gatewayClienteId = assinatura.getGatewayClienteId();
        if (gatewayClienteId == null) {
            gatewayClienteId = asaasService.criarCliente(empresa);
            assinatura.setGatewayClienteId(gatewayClienteId);
        }

        // Create gateway subscription
        String proxVencimento = LocalDate.now().plusDays(1).toString();
        String gatewaySubId = asaasService.criarAssinatura(gatewayClienteId, plano, proxVencimento);
        if (gatewaySubId != null) {
            assinatura.setGatewayAssinaturaId(gatewaySubId);
        }

        assinaturaRepository.save(assinatura);

        // Generate payment link
        String linkPagamento = asaasService.gerarLinkCheckout(gatewaySubId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assinatura", toMap(assinatura));
        result.put("linkPagamento", linkPagamento != null ? linkPagamento : "Configure ASAAS_API_KEY para gerar link de pagamento");
        return result;
    }

    @Transactional
    public Map<String, Object> upgrade(PlanoAssinatura novoPlano) {
        Long empresaId = requireEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        Assinatura assinatura = assinaturaRepository.findByEmpresaId(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assinatura não encontrada"));

        PlanoAssinatura planoAnterior = assinatura.getPlano();
        assinatura.setPlano(novoPlano);
        assinatura.setValorMensal(asaasService.getPreco(novoPlano));
        empresa.setPlano(novoPlano);

        empresaRepository.save(empresa);
        assinaturaRepository.save(assinatura);

        log.info("Empresa {} upgraded: {} → {}", empresaId, planoAnterior, novoPlano);
        return toMap(assinatura);
    }

    @Transactional
    public void cancelar() {
        Long empresaId = requireEmpresaId();
        Assinatura assinatura = assinaturaRepository.findByEmpresaId(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assinatura não encontrada"));

        asaasService.cancelarAssinatura(assinatura.getGatewayAssinaturaId());
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinaturaRepository.save(assinatura);
        log.info("Assinatura cancelada para empresa {}", empresaId);
    }

    @Transactional
    public void processarWebhook(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String event = node.path("event").asText();
            String subId = node.path("payment").path("subscription").asText(null);

            if (subId == null) return;

            assinaturaRepository.findByGatewayAssinaturaId(subId).ifPresent(assinatura -> {
                switch (event) {
                    case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> {
                        assinatura.setStatus(StatusAssinatura.ATIVA);
                        assinatura.setDataProximaCobranca(LocalDate.now().plusMonths(1));
                        if (assinatura.getEmpresa() != null) {
                            assinatura.getEmpresa().setPlano(assinatura.getPlano());
                            empresaRepository.save(assinatura.getEmpresa());
                        }
                        log.info("Pagamento recebido — assinatura {} ativada", subId);
                    }
                    case "PAYMENT_OVERDUE" -> {
                        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
                        log.warn("Pagamento em atraso — assinatura {}", subId);
                    }
                    case "SUBSCRIPTION_DELETED" -> {
                        assinatura.setStatus(StatusAssinatura.CANCELADA);
                        log.info("Assinatura {} cancelada via webhook", subId);
                    }
                }
                assinaturaRepository.save(assinatura);
            });
        } catch (Exception e) {
            log.error("Erro ao processar webhook Asaas: {}", e.getMessage());
        }
    }

    /** Daily at 6AM: expire trials and mark overdue subscriptions */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void verificarAssinaturasVencidas() {
        LocalDate hoje = LocalDate.now();

        assinaturaRepository.findByStatusAndDataProximaCobrancaBefore(StatusAssinatura.TRIAL, hoje)
            .forEach(a -> {
                a.setStatus(StatusAssinatura.INADIMPLENTE);
                assinaturaRepository.save(a);
                log.info("Trial expirado: empresa {}", a.getEmpresa() != null ? a.getEmpresa().getId() : "?");
            });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Assinatura criarAssinaturaTrialParaEmpresa(Long empresaId) {
        Empresa empresa = empresaRepository.getReferenceById(empresaId);
        Assinatura a = new Assinatura();
        a.setEmpresa(empresa);
        a.setPlano(PlanoAssinatura.STARTER);
        a.setValorMensal(asaasService.getPreco(PlanoAssinatura.STARTER));
        a.setStatus(StatusAssinatura.TRIAL);
        a.setDiasTrial(14);
        return assinaturaRepository.save(a);
    }

    private Map<String, Object> toMap(Assinatura a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("plano", a.getPlano());
        m.put("valorMensal", a.getValorMensal());
        m.put("status", a.getStatus());
        m.put("dataInicio", a.getDataInicio());
        m.put("dataProximaCobranca", a.getDataProximaCobranca());
        m.put("diasTrial", a.getDiasTrial());
        m.put("trialAtivo", a.isTrialAtivo());
        m.put("acesso", a.isAcesso());
        return m;
    }

    private Long requireEmpresaId() {
        Long id = TenantContext.getEmpresaId();
        if (id == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa não associada ao usuário");
        return id;
    }
}
