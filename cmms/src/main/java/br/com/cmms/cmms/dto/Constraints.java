package br.com.cmms.cmms.dto;

/**
 * Centralised Bean Validation patterns and limits. Keeping them in one
 * place stops the validators from drifting apart over time and lets the
 * frontend mirror the same regexes when wiring inline validations.
 */
public final class Constraints {

    private Constraints() {}

    // ── Field lengths ────────────────────────────────────────────────────
    public static final int NOME_MAX        = 120;
    public static final int EMAIL_MAX       = 255;
    public static final int SENHA_MIN       = 8;
    public static final int SENHA_MAX       = 100;
    public static final int CODIGO_MAX      = 50;
    public static final int TELEFONE_MAX    = 20;
    public static final int DESCRICAO_MAX   = 500;
    public static final int LOCALIZACAO_MAX = 100;
    public static final int ROLE_MAX        = 40;

    // ── Regex ────────────────────────────────────────────────────────────

    /**
     * Roles whitelisted by the application. Anything outside this list is
     * rejected at bind time before the service even runs.
     */
    public static final String ROLE_REGEX =
        "^ROLE_(SUPER_ADMIN|ADMIN|GESTOR|TECNICO|VISUALIZADOR)$";

    /**
     * Machine / maintenance statuses. Mirrors the values the UI emits.
     * Case-sensitive on purpose so typos surface at the API boundary.
     */
    public static final String MAQUINA_STATUS_REGEX = "^(ATIVO|INATIVO|EM_MANUTENCAO)$";
    public static final String MANUTENCAO_STATUS_REGEX =
        "^(ABERTA|EM_ANDAMENTO|CONCLUIDA|CANCELADA)$";

    /** Priority labels used across the system. */
    public static final String PRIORIDADE_REGEX = "^(CRITICA|ALTA|MEDIA|BAIXA)$";

    /** Maintenance kinds. */
    public static final String TIPO_MANUTENCAO_REGEX = "^(PREVENTIVA|CORRETIVA|PREDITIVA)$";

    /**
     * Free-form code (peças, ferramentas). Letters, digits, '-' and '_';
     * trims the surface for injection-style payloads.
     */
    public static final String CODIGO_REGEX = "^[A-Za-z0-9_\\-]{1," + CODIGO_MAX + "}$";

    /**
     * Phone (loose): digits, spaces, +, -, (, ). Frontend may format it
     * however it wants but the API only accepts these characters.
     */
    public static final String TELEFONE_REGEX = "^[0-9+\\-() ]{8,20}$";

    /**
     * Strong-password hint: at least 1 letter and 1 digit, length checked
     * by {@code @Size}. The frontend's strength meter does the polish.
     */
    public static final String SENHA_FORTE_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{" + SENHA_MIN + "," + SENHA_MAX + "}$";
}
