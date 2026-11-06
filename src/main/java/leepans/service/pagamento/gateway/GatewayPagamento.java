package leepans.service.pagamento.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Contrato que cada gateway de pagamento implementa. O resto do sistema só
 * conhece esta interface; a implementação em uso vem de app.pagamento.gateway.
 */
public interface GatewayPagamento {

    /** Identificador usado na configuração e na URL do webhook ("fake", "mercadopago"...). */
    String nome();

    /** @throws GatewayIndisponivelException em erro técnico. Recusa vem como status, não como exceção. */
    ResultadoCobranca criarCobranca(SolicitacaoCobranca solicitacao);

    ResultadoCobranca consultar(String gatewayPaymentId);

    ResultadoEstorno estornar(String gatewayPaymentId, BigDecimal valor);

    void cancelar(String gatewayPaymentId);

    /**
     * Valida a assinatura e traduz o corpo do webhook.
     *
     * @throws leepans.exception.AuthorizationException se a assinatura for inválida
     */
    EventoGateway interpretarWebhook(String corpo, Map<String, String> headers);
}
