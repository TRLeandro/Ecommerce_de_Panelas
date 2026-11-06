package leepans.model;

public enum StatusPagamento {
    PENDENTE(1L, "Pendente"),
    AGUARDANDO_PAGAMENTO(2L, "Aguardando pagamento"),
    AUTORIZADO(3L, "Autorizado"),
    PAGO(4L, "Pago"),
    RECUSADO(5L, "Recusado"),
    EXPIRADO(6L, "Expirado"),
    CANCELADO(7L, "Cancelado"),
    ESTORNADO(8L, "Estornado"),
    CONTESTADO(9L, "Contestado");

    private final Long id;
    private final String nome;

    StatusPagamento(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public static StatusPagamento valueOf(Long id){
        for(StatusPagamento status : StatusPagamento.values()){
            if(status.getId().equals(id)){
                return status;
            }
        }
        return null;
    }

    /** Pagamento ainda em andamento: um pedido só pode ter um nesse estado por vez. */
    public boolean isAtivo() {
        return this == PENDENTE || this == AGUARDANDO_PAGAMENTO || this == AUTORIZADO;
    }

    /** Estado sem nenhuma transição de saída. PAGO não é final: ainda pode ser estornado ou contestado. */
    public boolean isFinal() {
        return !isAtivo() && this != PAGO;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
