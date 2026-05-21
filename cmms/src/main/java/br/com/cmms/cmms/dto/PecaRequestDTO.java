package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para criar / atualizar uma peça.
 *
 * <p>Mantemos a classe POJO (em vez de record) porque o controller / service
 * existente espera getters mutáveis e isso reduz blast radius da mudança.
 */
@Schema(description = "Dados para cadastro ou atualização de uma peça do estoque.")
public class PecaRequestDTO {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = Constraints.NOME_MAX,
        message = "Nome deve ter entre 2 e " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Rolamento 6204-2RS")
    private String nome;

    @NotBlank(message = "Código é obrigatório.")
    @Pattern(regexp = Constraints.CODIGO_REGEX,
        message = "Código deve conter apenas letras, dígitos, '-' ou '_'.")
    @Schema(example = "ROL-6204")
    private String codigo;

    @NotNull(message = "Quantidade é obrigatória.")
    @PositiveOrZero(message = "Quantidade deve ser >= 0.")
    @Max(value = 1_000_000, message = "Quantidade absurdamente alta — verifique unidade.")
    @Schema(example = "50")
    private Integer quantidadeEmEstoque;

    @NotNull(message = "Custo unitário é obrigatório.")
    @Positive(message = "Custo unitário deve ser positivo.")
    @Schema(example = "29.90")
    private Double custoUnitario;

    @NotNull(message = "Vida útil em horas é obrigatória.")
    @PositiveOrZero(message = "Vida útil deve ser >= 0.")
    @Schema(example = "8000")
    private Integer vidaUtilHoras;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getQuantidadeEmEstoque() { return quantidadeEmEstoque; }
    public void setQuantidadeEmEstoque(Integer quantidadeEmEstoque) { this.quantidadeEmEstoque = quantidadeEmEstoque; }
    public Double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(Double custoUnitario) { this.custoUnitario = custoUnitario; }
    public Integer getVidaUtilHoras() { return vidaUtilHoras; }
    public void setVidaUtilHoras(Integer vidaUtilHoras) { this.vidaUtilHoras = vidaUtilHoras; }
}
