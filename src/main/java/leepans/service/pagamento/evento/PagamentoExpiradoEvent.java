package leepans.service.pagamento.evento;

public record PagamentoExpiradoEvent(Long pagamentoId, Long pedidoId) {
}
