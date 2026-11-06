package leepans.dto.pagamento;

import jakarta.validation.constraints.NotNull;
import leepans.model.MetodoPagamento;

public record IniciarPagamentoDTO(
        @NotNull(message = "O método de pagamento é obrigatório (PIX, BOLETO, CARTAO_CREDITO ou CARTAO_DEBITO)")
        MetodoPagamento metodo,

        String tokenCartao,

        Integer parcelas
) {
}
