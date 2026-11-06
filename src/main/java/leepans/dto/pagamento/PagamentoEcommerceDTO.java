package leepans.dto.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import leepans.model.MetodoPagamento;
import leepans.model.StatusPagamento;

public record PagamentoEcommerceDTO(
        Long id,
        Long pedidoId,
        MetodoPagamento metodo,
        StatusPagamento status,
        BigDecimal valor,
        LocalDateTime pagoEm
) {
}
