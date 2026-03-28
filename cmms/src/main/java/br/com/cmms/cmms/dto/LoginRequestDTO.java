package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Formato de email inválido")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 3, max = 100, message = "Senha deve ter entre 3 e 100 caracteres")
    private String senha;

    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
}
