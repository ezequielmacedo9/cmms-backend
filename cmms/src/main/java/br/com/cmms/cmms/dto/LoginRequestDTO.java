package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de login. Mantém o limite mínimo de senha frouxo (3 chars) para
 * não bloquear usuários legados criados com regras antigas — a política
 * de senha forte é aplicada apenas em fluxos de criação/troca.
 */
@Schema(description = "Credenciais de login.")
public class LoginRequestDTO {

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = Constraints.EMAIL_MAX, message = "Email muito longo.")
    @Schema(example = "ana@empresa.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 3, max = Constraints.SENHA_MAX,
        message = "Senha deve ter entre 3 e " + Constraints.SENHA_MAX + " caracteres.")
    @Schema(example = "minhaSenhaSegura123")
    private String senha;

    /** Código TOTP — obrigatório apenas quando o usuário tem 2FA habilitado. */
    @Size(max = 8, message = "Código 2FA inválido.")
    @Schema(example = "123456", description = "Código do autenticador (se 2FA ativo).")
    private String totpCode;

    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getTotpCode() { return totpCode; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setTotpCode(String totpCode) { this.totpCode = totpCode; }
}
