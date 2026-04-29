package br.com.cmms.cmms.model;

public enum PlanoAssinatura {
    STARTER(20, 3),
    PRO(100, 10),
    BUSINESS(500, 999),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int limiteAtivos;
    private final int limiteUsuarios;

    PlanoAssinatura(int limiteAtivos, int limiteUsuarios) {
        this.limiteAtivos = limiteAtivos;
        this.limiteUsuarios = limiteUsuarios;
    }

    public int getLimiteAtivos() { return limiteAtivos; }
    public int getLimiteUsuarios() { return limiteUsuarios; }
}
