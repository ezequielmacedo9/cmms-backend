package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Usuario;

import java.time.LocalDateTime;

public class UsuarioResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private String role;
    private boolean ativo;
    private LocalDateTime dataCriacao;

    public static UsuarioResponseDTO from(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.id          = u.getId();
        dto.nome        = u.getNome() != null ? u.getNome() : u.getEmail();
        dto.email       = u.getEmail();
        dto.role        = u.getRole() != null ? u.getRole().getNome() : null;
        dto.ativo       = u.isAtivo();
        dto.dataCriacao = u.getDataCriacao();
        return dto;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
