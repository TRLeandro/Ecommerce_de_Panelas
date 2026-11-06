package leepans.service.pagamento.evento;

public record PedidoCanceladoEvent(Long pedidoId, String motivo) {
}
