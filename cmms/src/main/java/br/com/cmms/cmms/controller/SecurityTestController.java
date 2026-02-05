package br.com.cmms.cmms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SecurityTestController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String rotaAdmin() {
        return "Acesso ADMIN";
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/user")
    public String rotaUser() {
        return "Acesso USER";
    }
}
