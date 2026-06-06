package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service sign-up payload: creates a new empresa (tenant) plus its first
 * administrator user in a single step.
 */
@Schema(description = "Cadastro de nova empresa + usuário administrador.")
public class RegistroRequestDTO {

    @NotBlank(message = "Nome da empresa é obrigatório.")
    @Size(max = 150, message = "Nome da empresa muito longo.")
    @Schema(example = "Indústria Acme Ltda")
    private String empresaNome;

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = Constraints.NOME_MAX, message = "Nome muito longo.")
    @Schema(example = "Ana Souza")
    private String nome;

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = Constraints.EMAIL_MAX, message = "Email muito longo.")
    @Schema(example = "ana@acme.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Pattern(regexp = Constraints.SENHA_FORTE_REGEX,
        message = "Senha deve ter ao menos 8 caracteres, com letras e números.")
    @Schema(example = "Acme2024forte")
    private String senha;

    public String getEmpresaNome() { return empresaNome; }
    public void setEmpresaNome(String empresaNome) { this.empresaNome = empresaNome; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
