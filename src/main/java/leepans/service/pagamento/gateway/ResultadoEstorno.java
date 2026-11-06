package leepans.service.pagamento.gateway;

public record ResultadoEstorno(boolean sucesso, String mensagem, String payloadBruto) {
}
