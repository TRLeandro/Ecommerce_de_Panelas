package leepans.service.pagamento.gateway;

/**
 * Erro técnico ao falar com o gateway (rede, timeout, 5xx). Não é recusa: a
 * situação da cobrança é desconhecida e o pagamento não muda de status.
 */
public class GatewayIndisponivelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GatewayIndisponivelException(String message) {
        super(message);
    }

    public GatewayIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
