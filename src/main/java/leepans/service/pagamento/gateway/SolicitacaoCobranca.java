package leepans.service.pagamento.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import leepans.model.MetodoPagamento;

public record SolicitacaoCobranca(
        Long pagamentoId,
        MetodoPagamento metodo,
        BigDecimal valor,
        Integer parcelas,
        String tokenCartao,
        String idempotencyKey,
        String descricao,
        DadosPagador pagador,
        LocalDateTime expiraEm
) {
}
