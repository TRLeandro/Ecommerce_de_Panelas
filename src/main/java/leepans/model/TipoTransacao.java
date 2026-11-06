package leepans.model;

public enum TipoTransacao {
    CRIACAO(1L, "Criação"),
    CONSULTA(2L, "Consulta"),
    WEBHOOK(3L, "Webhook"),
    ESTORNO(4L, "Estorno"),
    CANCELAMENTO(5L, "Cancelamento");

    private final Long id;
    private final String nome;

    TipoTransacao(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public static TipoTransacao valueOf(Long id) {
        for (TipoTransacao tipo : TipoTransacao.values()) {
            if (tipo.getId().equals(id)) {
                return tipo;
            }
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
