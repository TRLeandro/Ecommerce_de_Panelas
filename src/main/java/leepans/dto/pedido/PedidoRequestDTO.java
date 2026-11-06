package leepans.dto.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import leepans.dto.endereco.EnderecoRequestDTO;

public record PedidoRequestDTO(

        @NotEmpty(message = "O pedido deve conter pelo menos um item")
        @Valid
        List<ItemPedidoRequestDTO> itens,

        @Valid
        EnderecoRequestDTO endereco,

        String codigoCupomDesconto,

        Integer version
) {
}
