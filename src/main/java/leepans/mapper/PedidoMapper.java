package leepans.mapper;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import leepans.dto.pedido.EnderecoEntregaResponseDTO;
import leepans.dto.pedido.PedidoRequestDTO;
import leepans.dto.pedido.PedidoResponseDTO;
import leepans.model.EnderecoEntrega;
import leepans.model.ItemPedido;
import leepans.model.Pedido;

@ApplicationScoped
public class PedidoMapper {

    @Inject
    ItemPedidoMapper itemPedidoMapper;

    @Inject
    UsuarioMapper usuarioMapper;

    @Inject
    PagamentoMapper pagamentoMapper;

    public Pedido toEntity(PedidoRequestDTO dto) {
        if (dto == null) return null;

        Pedido pedido = new Pedido();
        List<ItemPedido> itens = dto.itens().stream()
                .map(itemPedidoMapper::toEntity)
                .toList();


        if (dto.version() != null) {
            pedido.setVersion(dto.version());
        }

        pedido.setItens(itens);

        return pedido;
    }

    public PedidoResponseDTO toResponse(Pedido pedido) {
        if (pedido == null) return null;

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getUsuario() != null ? usuarioMapper.toResponseDTO(pedido.getUsuario()) : null,
                toResponse(pedido.getEnderecoEntrega()),
                pedido.getStatus(),
                pedido.getValorItens(),
                pedido.getValorDesconto(),
                pedido.getValorFrete(),
                pedido.valorTotal(),
                CupomDescontoMapper.toResponse(pedido.getCupomDesconto()),
                pedido.getPagamentos().stream()
                        .map(pagamentoMapper::toResponse)
                        .toList(),
                pedido.getItens() != null ? pedido.getItens().stream()
                        .map(itemPedidoMapper::toResponse)
                        .toList() : null,
                pedido.getDataCadastro(),
                pedido.getVersion()
        );
    }

    private EnderecoEntregaResponseDTO toResponse(EnderecoEntrega endereco) {
        if (endereco == null) return null;

        return new EnderecoEntregaResponseDTO(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                endereco.getCep()
        );
    }
}
