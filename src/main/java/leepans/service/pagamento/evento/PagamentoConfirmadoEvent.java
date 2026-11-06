package leepans.service.pagamento.evento;

public record PagamentoConfirmadoEvent(Long pagamentoId, Long pedidoId) {
}
