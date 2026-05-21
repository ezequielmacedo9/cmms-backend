package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Alterar a role de um usuário existente.")
public class AlterarRoleRequestDTO {

    @NotBlank(message = "Role é obrigatória.")
    @Pattern(regexp = Constraints.ROLE_REGEX,
        message = "Role deve ser uma das: ROLE_SUPER_ADMIN, ROLE_ADMIN, "
                + "ROLE_GESTOR, ROLE_TECNICO, ROLE_VISUALIZADOR.")
    @Schema(example = "ROLE_GESTOR",
        allowableValues = {"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_GESTOR",
                           "ROLE_TECNICO", "ROLE_VISUALIZADOR"})
    private String roleNome;

    public String getRoleNome() { return roleNome; }
    public void setRoleNome(String roleNome) { this.roleNome = roleNome; }
}
