package leepans.service.ecommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import leepans.exception.BusinessRuleViolationException;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.Cidade;
import leepans.model.CupomDesconto;
import leepans.model.Endereco;
import leepans.model.EnderecoEntrega;
import leepans.model.ItemPedido;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPedido;
import leepans.model.Usuario;
import leepans.repository.CidadeRepository;
import leepans.repository.CupomDescontoRepository;
import leepans.repository.ItemPedidoRepository;
import leepans.repository.PedidoRepository;
import leepans.service.pagamento.evento.PedidoCanceladoEvent;
import leepans.service.pagamento.evento.PedidoCriadoEvent;
import leepans.service.pagamento.gateway.EstornoNegadoException;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;

@ApplicationScoped
public class PedidoService implements PedidoServiceInter {

    @Inject
    PedidoRepository repository;

    @Inject
    ItemPedidoRepository itemRepository;

    @Inject
    CupomDescontoRepository cupomRepository;

    @Inject
    UsuarioService usuarioService;

    @Inject
    CidadeRepository cidadeRepository;

    @Inject
    PagamentoService pagamentoService;

    @Inject
    CupomDescontoService cupomDescontoService;

    @Inject
    Event<PedidoCriadoEvent> pedidoCriado;

    @Inject
    Event<PedidoCanceladoEvent> pedidoCancelado;

    @Override
    public List<Pedido> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Pedido findById(Long id) {
        Pedido pedido = repository.findById(id);
        if (pedido == null) {
            throw new ResourceNotFoundException("Pedido", id);
        }
        return pedido;
    }

    @Override
    public Pedido findByIdDoUsuario(Long id, String login) {
        Pedido pedido = repository.findByIdDoUsuario(id, login);
        if (pedido == null) {
            // Mesma resposta para "não existe" e "é de outro usuário": não revela a existência do ID.
            throw new ResourceNotFoundException("Pedido", id);
        }
        return pedido;
    }

    @Override
    public List<Pedido> findCompras(String usuarioLogin, StatusPedido status) {
        return repository.findComprasUsuario(usuarioLogin, status).list();
    }

    @Override
    public List<Pedido> findCompras(String usuarioLogin) {
        return repository.findComprasUsuario(usuarioLogin).list();
    }

    @Override
    public List<Pedido> findByUsuarioId(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).list();
    }

    @Override
    public List<Pedido> findByStatus(StatusPedido status) {
        return repository.findByStatus(status).list();
    }

    @Override
    public List<Pedido> findByEnderecoCidade(String cidade) {
        return repository.findByEnderecoCidade(cidade).list();
    }

    @Override
    @Transactional
    public Pedido create(Pedido pedido, String login, Endereco endereco, String codigoCupom) {
        validarItens(pedido.getItens());

        // O preço vem sempre do catálogo, nunca do cliente
        pedido.getItens().forEach(item -> item.setValorUnitario(item.getPanela().getPreco()));
        BigDecimal valorItens = calcularValorItens(pedido.getItens());

        pedido.setCupomDesconto(buscarCupomComTrava(codigoCupom));
        BigDecimal valorDesconto = validarEAplicarCupomDesconto(pedido.getCupomDesconto(), valorItens);

        Usuario usuario = usuarioService.findByLogin(login);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuário", login);
        }

        pedido.setUsuario(usuario);
        pedido.setEnderecoEntrega(copiarEnderecoEntrega(endereco, usuario));
        pedido.setValorItens(valorItens);
        pedido.setValorDesconto(valorDesconto);

        // TODO estoque: reservar os itens do pedido
        repository.persist(pedido);

        pedidoCriado.fire(new PedidoCriadoEvent(pedido.getId()));

        return pedido;
    }

    @Override
    @Transactional
    public void iniciarSeparacao(Long id) {
        findById(id).iniciarSeparacao();
    }

    @Override
    @Transactional
    public void marcarComoEnviado(Long id) {
        findById(id).marcarComoEnviado();
    }

    @Override
    @Transactional
    public void marcarComoEntregue(Long id) {
        findById(id).marcarComoEntregue();
    }

    @Override
    @Transactional(dontRollbackOn = { GatewayIndisponivelException.class, EstornoNegadoException.class })
    public void cancelar(Long id) {
        Pedido pedido = findById(id);
        repository.travar(pedido);

        // Antes de qualquer chamada ao gateway: nada de devolver dinheiro de pedido que não pode ser cancelado
        if (!pedido.podeCancelar()) {
            throw new BusinessRuleViolationException(
                    "Não é possível cancelar um pedido com status " + pedido.getStatus() + ".", "status");
        }

        Optional<Pagamento> aprovado = pedido.pagamentoAprovado();
        if (aprovado.isPresent()) {
            // O estorno já cancela o pedido e devolve o cupom
            pagamentoService.estornar(aprovado.get().getId());
            return;
        }

        pedido.pagamentoAtivo().ifPresent(pagamentoService::cancelarNoGateway);
        pedido.cancelar();
        // TODO estoque: liberar os itens do pedido
        if (pedido.getCupomDesconto() != null) {
            cupomDescontoService.incrementarQuantidade(pedido.getCupomDesconto());
        }
        pedidoCancelado.fire(new PedidoCanceladoEvent(pedido.getId(), "Pedido cancelado pela loja."));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pedido pedido = findById(id);

        if (!pedido.getPagamentos().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Pedido com pagamentos não pode ser removido: pagamentos nunca são apagados.", "pagamentos");
        }

        // Deletar itens do pedido
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                itemRepository.deleteById(item.getId());
            }
        }

        repository.deleteById(id);
    }

    private BigDecimal calcularValorItens(List<ItemPedido> itens) {
        BigDecimal valorItens = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
            valorItens = valorItens.add(item.getValorUnitario().multiply(quantidade));
        }
        return valorItens;
    }

    /**
     * Valida os itens do pedido
     */
    private void validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ValidationException("O pedido deve conter pelo menos um item.", "itens");
        }

        for (ItemPedido item : itens) {
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new ValidationException("A quantidade de cada item deve ser maior que zero.", "quantidade");
            }
            if (item.getPanela() == null) {
                throw new ValidationException("Panela não encontrada.", "idPanela");
            }
        }
    }

    private CupomDesconto buscarCupomComTrava(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        CupomDesconto cupom = cupomRepository.findByCodigoParaAtualizacao(codigoCupom.trim());
        if (cupom == null) {
            throw new ValidationException(
                    "Cupom de desconto com código '" + codigoCupom.trim() + "' não encontrado.",
                    "codigoCupomDesconto");
        }
        return cupom;
    }

    /**
     * Valida e aplica o cupom desconto ao pedido
     */
    private BigDecimal validarEAplicarCupomDesconto(CupomDesconto cupomDesconto, BigDecimal valorItens) {
        if (cupomDesconto == null) {
            return BigDecimal.ZERO;
        }

        if (!cupomDesconto.isAtivo()) {
            throw new ValidationException("O cupom desconto está inativo.", "cupomDesconto");
        }

        if (cupomDesconto.getDataValidade() != null && cupomDesconto.getDataValidade().isBefore(LocalDateTime.now())) {
            throw new ValidationException("O cupom desconto expirou.", "cupomDesconto");
        }

        if (cupomDesconto.getQuantidadeDisponivel() != null && cupomDesconto.getQuantidadeDisponivel() <= 0) {
            throw new ValidationException("O cupom desconto não possui quantidade disponível.", "cupomDesconto");
        }

        if (cupomDesconto.getValorMinimoCompra() != null
                && cupomDesconto.getValorMinimoCompra().compareTo(valorItens) > 0) {
            throw new ValidationException(
                    "O valor mínimo de compra para este cupom é R$ " + cupomDesconto.getValorMinimoCompra(),
                    "cupomDesconto");
        }

        BigDecimal desconto = BigDecimal.ZERO;

        if (cupomDesconto.getValorDesconto() != null
                && cupomDesconto.getValorDesconto().compareTo(BigDecimal.ZERO) > 0) {
            desconto = cupomDesconto.getValorDesconto();
        } else if (cupomDesconto.getPercentualDesconto() != null
                && cupomDesconto.getPercentualDesconto().compareTo(new BigDecimal(0)) > 0) {
            BigDecimal percentual = cupomDesconto.getPercentualDesconto();
            desconto = valorItens.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (desconto.compareTo(BigDecimal.ZERO) > 0) {
            if (cupomDesconto.getQuantidadeDisponivel() != null) {
                cupomDesconto.setQuantidadeDisponivel(cupomDesconto.getQuantidadeDisponivel() - 1);
            }
            return desconto.min(valorItens);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Copia para o pedido o endereço informado ou, na falta dele, o do usuário.
     */
    private EnderecoEntrega copiarEnderecoEntrega(Endereco endereco, Usuario usuario) {
        if (endereco == null) {
            if (usuario.getEndereco() == null) {
                throw new ValidationException(
                        "Endereço não fornecido e usuário não possui endereço registrado.",
                        "endereco");
            }
            return EnderecoEntrega.copiaDe(usuario.getEndereco(), usuario.getEndereco().getCidade());
        }

        // Validar campos obrigatórios do endereço
        if (endereco.getLogradouro() == null || endereco.getLogradouro().isBlank()) {
            throw new ValidationException("O logradouro do endereço é obrigatório.", "endereco.logradouro");
        }
        if (endereco.getNumero() == null || endereco.getNumero().isBlank()) {
            throw new ValidationException("O número do endereço é obrigatório.", "endereco.numero");
        }
        if (endereco.getBairro() == null || endereco.getBairro().isBlank()) {
            throw new ValidationException("O bairro do endereço é obrigatório.", "endereco.bairro");
        }
        if (endereco.getCep() == null || endereco.getCep().isBlank()) {
            throw new ValidationException("O CEP do endereço é obrigatório.", "endereco.cep");
        }
        if (endereco.getCidade() == null || endereco.getCidade().getId() == null) {
            throw new ValidationException("A cidade do endereço é obrigatória.", "endereco.cidade");
        }

        Cidade cidade = cidadeRepository.findById(endereco.getCidade().getId());
        if (cidade == null) {
            throw new ResourceNotFoundException("Cidade", endereco.getCidade().getId());
        }
        return EnderecoEntrega.copiaDe(endereco, cidade);
    }
}
