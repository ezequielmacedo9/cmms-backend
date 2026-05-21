package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para convidar um novo usuário ao sistema.
 *
 * <p>Validações endurecidas:
 * <ul>
 *   <li>Senha de pelo menos {@value Constraints#SENHA_MIN} caracteres com
 *       letra + dígito (regex {@link Constraints#SENHA_FORTE_REGEX}).</li>
 *   <li>Role restrita ao catálogo da aplicação ({@link Constraints#ROLE_REGEX}).</li>
 * </ul>
 */
@Schema(description = "Dados para convite/criação de novo usuário.")
public class ConvidarUsuarioRequestDTO {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = Constraints.NOME_MAX,
        message = "Nome deve ter entre 2 e " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Ana Silva")
    private String nome;

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = Constraints.EMAIL_MAX, message = "Email muito longo.")
    @Schema(example = "ana@empresa.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = Constraints.SENHA_MIN, max = Constraints.SENHA_MAX,
        message = "Senha deve ter entre " + Constraints.SENHA_MIN + " e "
                + Constraints.SENHA_MAX + " caracteres.")
    @Pattern(regexp = Constraints.SENHA_FORTE_REGEX,
        message = "Senha deve conter pelo menos 1 letra e 1 número.")
    @Schema(description = "Mínimo de " + Constraints.SENHA_MIN
                        + " caracteres, contendo letras e números.")
    private String senha;

    @NotBlank(message = "Role é obrigatória.")
    @Pattern(regexp = Constraints.ROLE_REGEX,
        message = "Role deve ser uma das: ROLE_SUPER_ADMIN, ROLE_ADMIN, "
                + "ROLE_GESTOR, ROLE_TECNICO, ROLE_VISUALIZADOR.")
    @Schema(example = "ROLE_TECNICO",
        allowableValues = {"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_GESTOR",
                           "ROLE_TECNICO", "ROLE_VISUALIZADOR"})
    private String roleNome;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getRoleNome() { return roleNome; }
    public void setRoleNome(String roleNome) { this.roleNome = roleNome; }
}
