package br.com.cmms.cmms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Operacional")
@SecurityRequirements
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Keep-alive / liveness simples",
        description = "Endpoint público usado pelo scheduler do Render para impedir cold-start.")
    public String ping() {
        return "Pong! Endpoint de teste funcionando.";
    }
}