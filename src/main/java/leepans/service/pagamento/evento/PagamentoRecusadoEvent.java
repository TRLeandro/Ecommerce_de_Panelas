package leepans.service.pagamento.evento;

public record PagamentoRecusadoEvent(Long pagamentoId, Long pedidoId, String motivo) {
}
