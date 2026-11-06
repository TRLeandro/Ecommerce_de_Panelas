package leepans.dto.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import leepans.model.MetodoPagamento;
import leepans.model.StatusPagamento;

public record PagamentoResponseDTO(
        Long id,
        Long pedidoId,
        MetodoPagamento metodo,
        StatusPagamento status,
        BigDecimal valor,
        String gateway,
        String gatewayPaymentId,
        String motivoRecusa,
        LocalDateTime expiraEm,
        LocalDateTime pagoEm,
        LocalDateTime canceladoEm,
        CartaoResponseDTO cartao,
        PixResponseDTO pix,
        BoletoResponseDTO boleto,
        LocalDateTime dataCadastro,
        Integer version
) {
}
