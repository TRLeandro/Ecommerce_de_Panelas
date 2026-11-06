package leepans.service.pagamento.gateway;

public record EventoGateway(String eventoId, String gatewayPaymentId, StatusGateway novoStatus, String payloadBruto) {
}
