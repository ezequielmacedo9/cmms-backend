package br.com.cmms.cmms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Liveness probe consumido pelo frontend ({@code WakeupService.ping()}) para
 * detectar quando a instancia do Render terminou o cold start. Enquanto esse
 * probe nao responde 200, a tela de login fica bloqueada com "Servidor
 * iniciando".
 *
 * <p>Deliberadamente separado de {@code /actuator/health}: o agregado do
 * Actuator fica DOWN se qualquer indicador auxiliar falhar (e-mail, cache,
 * disco), o que travaria o login por um motivo que nao impede a API de
 * atender. Aqui a resposta 200 significa apenas "o contexto Spring subiu e
 * esta servindo requisicoes".
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "timestamp", Instant.now().toString()
        ));
    }
}
