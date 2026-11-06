package leepans.model;

public enum MetodoPagamento {
    CARTAO_CREDITO(1L, "Cartão de Crédito"),
    CARTAO_DEBITO(2L, "Cartão de Débito"),
    BOLETO(3L, "Boleto"),
    PIX(4L, "Pix");

    private final Long id;
    private final String nome;

    MetodoPagamento(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public static MetodoPagamento valueOf(Long id) {
        for (MetodoPagamento metodo : MetodoPagamento.values()) {
            if (metodo.getId().equals(id)) {
                return metodo;
            }
        }
        return null;
    }

    public boolean isCartao() {
        return this == CARTAO_CREDITO || this == CARTAO_DEBITO;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
