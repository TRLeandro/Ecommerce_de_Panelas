package leepans.service.ecommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.Boleto;
import leepans.model.CartaoCredito;
import leepans.model.CartaoDebito;
import leepans.model.CupomDesconto;
import leepans.model.Endereco;
import leepans.model.ItemPedido;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.Pix;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;
import leepans.model.TipoPagamento;
import leepans.model.Usuario;
import leepans.repository.CupomDescontoRepository;
import leepans.repository.EnderecoRepository;
import leepans.repository.ItemPedidoRepository;
import leepans.repository.PedidoRepository;
import leepans.service.auth.EmailService;

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
    EnderecoService enderecoService;

    @Inject
    EnderecoRepository enderecoRepository;

    @Inject
    PanelaService panelaService;

    @Inject
    PagamentoService pagamentoService;

    @Inject
    CupomDescontoService cupomDescontoService;

    @Inject
    EmailService emailService;

    @Inject
    PedidoService self;

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
    public Pedido create(Pedido pedido, String login, Endereco endereco, TipoPagamento tipoPagamento,
            String codigoCupom) {
        // Validar itens do pedido
        try {
            validarItens(pedido.getItens());

            // O preço vem sempre do catálogo, nunca do cliente
            pedido.getItens().forEach(item -> item.setValorUnitario(item.getPanela().getPreco()));

            pedido.setCupomDesconto(buscarCupomComTrava(codigoCupom));

            // Calcular valor bruto
            BigDecimal valorBruto = calcularValorBruto(pedido.getItens());

            // Validar e aplicar cupom desconto
            BigDecimal valorDesconto = validarEAplicarCupomDesconto(pedido.getCupomDesconto(), valorBruto);

            // Buscar o usuário
            Usuario usuario = usuarioService.findByLogin(login);
            if (usuario == null) {
                throw new ResourceNotFoundException("Usuário", login);
            }

            // Validar e processar endereço
            Endereco enderecoFinal = validarEProcessarEndereco(endereco, usuario);

            // Criar e configurar pagamento
            Pagamento pagamento = criarPagamento(pedido, tipoPagamento);
            pagamento.setValor(valorBruto.subtract(valorDesconto));

            // Configurar pedido
            pedido.setUsuario(usuario);
            pedido.setEndereco(enderecoFinal);
            pedido.setValorBruto(valorBruto);
            pedido.setValorDesconto(valorDesconto);
            pedido.setPagamento(pagamento);
            pedido.setStatus(StatusPedido.PENDENTE);

            // Persistir pedido e pagamento
            repository.persist(pedido);
            pagamentoService.create(pagamento);

            // Enviar email de confirmação para o cliente
            emailService.sendOrderConfirmedEmail(pedido.getUsuario().getNome(), pedido.getId().toString());

            return pedido;
        } catch (Exception e) {
            self.cancelarPedido(pedido, e);
            throw e;
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void cancelarPedido(Pedido pedido, Exception e) {
        if (pedido.getId() != null) {
            pedido.setStatus(StatusPedido.CANCELADO);
            repository.getEntityManager().merge(pedido);
        }
        String nome = pedido.getUsuario() != null ? pedido.getUsuario().getNome() : "Cliente";
        String idPedido = pedido.getId() != null ? pedido.getId().toString() : "N/A";
        emailService.sendOrderDeclinedEmail(nome, idPedido, e.getMessage());
    }

    @Override
    @Transactional
    public void setStatus(Long id, StatusPedido status) {
        Pedido pedido = findById(id);
        pedido.setStatus(status);
        repository.persist(pedido);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pedido pedido = findById(id);

        // Deletar itens do pedido
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                itemRepository.deleteById(item.getId());
            }
        }

        repository.deleteById(id);
    }

    private Pagamento criarPagamento(Pedido pedido, TipoPagamento tipoPagamento) {
        Pagamento pagamento = null;
        if (tipoPagamento == TipoPagamento.CARTAO_CREDITO)
            pagamento = new CartaoCredito();
        else if (tipoPagamento == TipoPagamento.CARTAO_DEBITO)
            pagamento = new CartaoDebito();
        else if (tipoPagamento == TipoPagamento.BOLETO)
            pagamento = new Boleto();
        else
            pagamento = new Pix();
        pagamento.setPedido(pedido);
        pagamento.setTipoPagamento(tipoPagamento);
        pagamento.setStatusPagamento(StatusPagamento.PENDENTE);
        pagamento.setDataProcessado(LocalDateTime.now());
        return pagamento;
    }

    private BigDecimal calcularValorBruto(List<ItemPedido> itens) {
        BigDecimal valorBruto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
            valorBruto = valorBruto.add(item.getValorUnitario().multiply(quantidade));
        }
        return valorBruto;
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
    @Transactional
    public BigDecimal validarEAplicarCupomDesconto(CupomDesconto cupomDesconto, BigDecimal valorBruto) {
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
                && cupomDesconto.getValorMinimoCompra().compareTo(valorBruto) > 0) {
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
            desconto = valorBruto.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (desconto.compareTo(BigDecimal.ZERO) > 0) {
            // cupomDescontoService.decrementarQuantidade(cupomDesconto);
            if (cupomDesconto.getQuantidadeDisponivel() > 0) {
                cupomDesconto.setQuantidadeDisponivel(cupomDesconto.getQuantidadeDisponivel() - 1);
            }
            return desconto;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Valida e processa o endereço do pedido
     */
    private Endereco validarEProcessarEndereco(Endereco endereco, Usuario usuario) {
        // Se não foi fornecido endereço, usar o endereço do usuário
        if (endereco == null) {
            if (usuario.getEndereco() == null) {
                throw new ValidationException(
                        "Endereço não fornecido e usuário não possui endereço registrado.",
                        "endereco");
            }
            return usuario.getEndereco();
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

        // Verificar se o endereço já existe no banco de dados
        Endereco enderecoExistente = enderecoRepository.findByAllFields(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCep(),
                endereco.getCidade().getId());

        if (enderecoExistente != null) {
            // Se o endereço já existe, usar o existente
            return enderecoExistente;
        }

        // Se não existe, criar um novo endereço
        enderecoService.create(endereco);
        return endereco;
    }
}
