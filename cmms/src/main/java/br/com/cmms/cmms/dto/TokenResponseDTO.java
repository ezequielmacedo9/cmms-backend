package br.com.cmms.cmms.dto;

public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String nome;
    private Long userId;

    public TokenResponseDTO(String accessToken, String refreshToken, String role, String nome, Long userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.nome = nome;
        this.userId = userId;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getRole() { return role; }
    public String getNome() { return nome; }
    public Long getUserId() { return userId; }
}
