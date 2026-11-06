package leepans.model;

public enum StatusPedido {
    AGUARDANDO_PAGAMENTO(1L, "Aguardando pagamento"),
    PAGO(2L, "Pago"),
    EM_SEPARACAO(3L, "Em separação"),
    ENVIADO(4L, "Enviado"),
    ENTREGUE(5L, "Entregue"),
    CANCELADO(6L, "Cancelado");

    private final Long id;
    private final String nome;

    StatusPedido(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public static StatusPedido valueOf(Long id){
        for(StatusPedido status : StatusPedido.values()){
            if(status.getId().equals(id)){
                return status;
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
