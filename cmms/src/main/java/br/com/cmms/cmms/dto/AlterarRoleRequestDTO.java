package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.NotBlank;

public class AlterarRoleRequestDTO {

    @NotBlank
    private String roleNome;

    public String getRoleNome() { return roleNome; }
    public void setRoleNome(String roleNome) { this.roleNome = roleNome; }
}
