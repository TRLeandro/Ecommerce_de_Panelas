package leepans.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import leepans.dto.cupomDesconto.CupomDescontoResponseDTO;
import leepans.dto.pagamento.PagamentoResponseDTO;
import leepans.dto.usuario.UsuarioResponseDTO;
import leepans.model.StatusPedido;

public record PedidoResponseDTO(
        Long id,
        UsuarioResponseDTO usuario,
        EnderecoEntregaResponseDTO enderecoEntrega,
        StatusPedido status,
        BigDecimal valorItens,
        BigDecimal valorDesconto,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        CupomDescontoResponseDTO cupomDesconto,
        List<PagamentoResponseDTO> pagamentos,
        List<ItemPedidoResponseDTO> itens,
        LocalDateTime dataCadastro,
        Integer version
) {
}
