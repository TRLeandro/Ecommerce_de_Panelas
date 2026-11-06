package leepans.service.pagamento.gateway;

/** Status normalizado devolvido pelos adaptadores. Interno: não é persistido. */
public enum StatusGateway {
    PENDENTE,
    AGUARDANDO_PAGAMENTO,
    AUTORIZADO,
    PAGO,
    RECUSADO,
    EXPIRADO,
    CANCELADO,
    ESTORNADO
}
