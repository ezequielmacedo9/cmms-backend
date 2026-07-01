package br.com.cmms.cmms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A file attached to a work order (photo, report...). The bytes are stored as
 * base64 in the database for now — fine for the small files a work order needs;
 * migrating to object storage (S3/blob) later only touches this entity + the
 * read/write paths.
 */
@Entity
@Table(name = "manutencao_anexo", indexes = {
    @Index(name = "idx_anexo_manutencao", columnList = "manutencao_id"),
    @Index(name = "idx_anexo_empresa", columnList = "empresa_id")
})
public class ManutencaoAnexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "manutencao_id", nullable = false)
    private Long manutencaoId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "content_type", length = 120)
    private String contentType;

    /** Size of the original file in bytes (for display/validation). */
    @Column
    private int tamanho;

    /** Base64-encoded file content. */
    @Column(name = "dados_base64", columnDefinition = "TEXT", nullable = false)
    private String dadosBase64;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void onCreate() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }

    public ManutencaoAnexo() {}

    public ManutencaoAnexo(Long empresaId, Long manutencaoId, String nome, String contentType,
                           int tamanho, String dadosBase64) {
        this.empresaId = empresaId;
        this.manutencaoId = manutencaoId;
        this.nome = nome;
        this.contentType = contentType;
        this.tamanho = tamanho;
        this.dadosBase64 = dadosBase64;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getManutencaoId() { return manutencaoId; }
    public void setManutencaoId(Long manutencaoId) { this.manutencaoId = manutencaoId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public int getTamanho() { return tamanho; }
    public void setTamanho(int tamanho) { this.tamanho = tamanho; }
    public String getDadosBase64() { return dadosBase64; }
    public void setDadosBase64(String dadosBase64) { this.dadosBase64 = dadosBase64; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
