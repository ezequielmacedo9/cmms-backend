package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ConfiguracaoSistemaDTO;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.ConfiguracaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoService service;
    private final AuditService audit;

    public ConfiguracaoController(ConfiguracaoService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    public ResponseEntity<List<ConfiguracaoSistemaDTO>> listar() {
        return ResponseEntity.ok(
            service.listar().stream().map(ConfiguracaoSistemaDTO::from).toList()
        );
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> salvar(
            @RequestBody Map<String, String> values,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        service.salvar(values);
        audit.log(ud.getUsername(), null, "CONFIG_UPDATE", "CONFIGURACAO", null,
            "Configurações atualizadas: " + values.keySet(),
            AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "Configurações salvas"));
    }
}
