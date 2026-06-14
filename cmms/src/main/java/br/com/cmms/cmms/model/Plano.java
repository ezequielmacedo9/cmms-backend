package br.com.cmms.cmms.model;

/**
 * Subscription tiers. Plans are fixed configuration (not a DB table) — the
 * limits and price live in code so they're versioned and testable. A limit of
 * {@code -1} means "unlimited".
 */
public enum Plano {

    STARTER   (15,  3,  149.90),
    PRO       (50,  10, 349.90),
    BUSINESS  (200, 30, 899.90),
    ENTERPRISE(-1,  -1, 1990.00);

    public static final int UNLIMITED = -1;

    private final int limiteAtivos;
    private final int limiteUsuarios;
    private final double valorMensal;

    Plano(int limiteAtivos, int limiteUsuarios, double valorMensal) {
        this.limiteAtivos = limiteAtivos;
        this.limiteUsuarios = limiteUsuarios;
        this.valorMensal = valorMensal;
    }

    public int getLimiteAtivos()    { return limiteAtivos; }
    public int getLimiteUsuarios()  { return limiteUsuarios; }
    public double getValorMensal()  { return valorMensal; }

    public boolean ativosIlimitados()   { return limiteAtivos == UNLIMITED; }
    public boolean usuariosIlimitados()  { return limiteUsuarios == UNLIMITED; }

    /** Display value for the UI: the number, or "Ilimitado" when unbounded. */
    public Object limiteAtivosLabel()   { return ativosIlimitados()   ? "Ilimitado" : limiteAtivos; }
    public Object limiteUsuariosLabel() { return usuariosIlimitados() ? "Ilimitado" : limiteUsuarios; }

    public static Plano fromNome(String nome) {
        try {
            return Plano.valueOf(nome);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
