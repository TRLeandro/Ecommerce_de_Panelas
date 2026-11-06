package leepans.service.pagamento.gateway;

import java.time.LocalDateTime;

import leepans.model.DadosBoleto;
import leepans.model.DadosCartao;
import leepans.model.DadosPix;

public record ResultadoCobranca(
        String gatewayPaymentId,
        StatusGateway status,
        String codigoRetorno,
        String mensagem,
        DadosCartao cartao,
        DadosPix pix,
        DadosBoleto boleto,
        LocalDateTime expiraEm,
        String payloadBruto
) {
}
