package leepans.service.ecommerce;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import leepans.dto.pagamento.BoletoRequestDTO;
import leepans.dto.pagamento.CartaoRequestDTO;
import leepans.dto.pagamento.PagamentoPatchDTO;
import leepans.dto.pagamento.PixRequestDTO;
import leepans.exception.BusinessRuleViolationException;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.Boleto;
import leepans.model.CartaoCredito;
import leepans.model.CartaoDebito;
import leepans.model.CupomDesconto;
import leepans.model.ListaDesejo;
import leepans.model.Pagamento;
import leepans.model.Pix;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;
import leepans.model.Usuario;
import leepans.repository.PagamentoRepository;
import leepans.service.auth.EmailService;

@ApplicationScoped
public class PagamentoService implements PagamentoServiceInter {

    @Inject
    PagamentoRepository repository;

    @Inject
    CupomDescontoService cupomService;

    @Inject
    ListaDesejoService listaDesejoService;

    @Inject
    EmailService emailService;

    @Override
    public List<Pagamento> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Pagamento findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Pagamento findByIdDoUsuario(Long id, String login) {
        Pagamento pagamento = repository.findByIdDoUsuario(id, login);
        if (pagamento == null) {
            // Mesma resposta para "não existe" e "é de outro usuário": não revela a existência do ID.
            throw new ResourceNotFoundException("Pagamento", id);
        }
        return pagamento;
    }

    @Override
    public List<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento) {
        return repository.findByStatusPagamento(statusPagamento).list();
    }

    @Override
    public List<Pagamento> findByUsuario(String login) {
        return repository.findByUsuario(login).list();
    }

    @Override
    @Transactional
    public Pagamento create(Pagamento pagamento) {
        repository.persist(pagamento);
        return pagamento;
    }

    @Override
    @Transactional
    public void processarPagamento(Pagamento pagamento) {
        // Validar se o pagamento existe
        if (pagamento == null || pagamento.getId() == null) {
            throw new ValidationException("Pagamento inválido ou não persistido.", "pagamento");
        }

        if (pagamento.getStatusPagamento() == StatusPagamento.RECUSADO && pagamento.getTentativasProcessamento() >= 3) {
            throw new ValidationException("Pagamento já foi recusado 3 vezes. O pedido foi cancelado. Crie um novo pedido.", "statusPagamento");
        }

        // Buscar o pagamento atualizado no banco
        Pagamento pagamentoAtual = repository.findById(pagamento.getId());
        if (pagamentoAtual == null) {
            throw new ResourceNotFoundException("Pagamento", pagamento.getId());
        }

        try {

            // Validar se todos os dados necessários foram preenchidos
            validarDadosPagamento(pagamentoAtual);

            if (!isItensPriceRight(pagamentoAtual))
                throw new BusinessRuleViolationException(
                        "O valor total do pedido não confere com o valor atualizado dos produtos", "valor");

            // Atualizar status para APROVADO e registrar data/hora do processamento
            pagamentoAtual.setStatusPagamento(StatusPagamento.APROVADO);
            pagamentoAtual.setDataProcessado(LocalDateTime.now());

            // Persistir as alterações
            repository.persist(pagamentoAtual);

            // Verificar e remover itens da lista de desejo do usuário
            removerItensListaDesejo(pagamentoAtual);

            if (pagamentoAtual.getPedido().getCupomDesconto() != null)
                atualizarCupom(pagamentoAtual.getPedido().getCupomDesconto(), true);

            // Enviar email de confirmação para o cliente
            emailService.sendPaymentApprovedEmail(pagamentoAtual.getPedido().getUsuario().getNome(),
                    pagamentoAtual.getId().toString());
        } catch (Exception e) {
            pagamentoAtual.setStatusPagamento(StatusPagamento.RECUSADO);
            pagamentoAtual.setTentativasProcessamento(pagamentoAtual.getTentativasProcessamento() + 1);

            if (pagamentoAtual.getTentativasProcessamento() >= 3) {
                pagamentoAtual.getPedido().setStatus(StatusPedido.CANCELADO);
                if (pagamentoAtual.getPedido().getCupomDesconto() != null)
                    atualizarCupom(pagamentoAtual.getPedido().getCupomDesconto(), false);
            }

            emailService.sendPaymentRefusedEmail(pagamentoAtual.getPedido().getUsuario().getNome(),
                    pagamentoAtual.getId().toString(), e.getMessage());
        }
    }

    @Transactional
    public void completeInfo(Long id, CartaoRequestDTO dto) {
        Pagamento pagamento = repository.findById(id);

        if (dto.isCredito()) {
            if (!(pagamento instanceof CartaoCredito)) {
                throw new IllegalStateException(
                        "O pagamento não é um cartão de crédito.");
            }
            CartaoCredito cartaoCredito = (CartaoCredito) pagamento;
            cartaoCredito.setNumero(dto.numero());
            cartaoCredito.setTitular(dto.titular());
            cartaoCredito.setValidade(dto.validade());
            cartaoCredito.setCodigoSeguranca(dto.codigoSeguranca());
        } else {
            if (!(pagamento instanceof CartaoDebito)) {
                throw new IllegalStateException(
                        "O pagamento não é um cartão de débito.");
            }
            CartaoDebito cartaoDebito = (CartaoDebito) pagamento;
            cartaoDebito.setNumero(dto.numero());
            cartaoDebito.setTitular(dto.titular());
            cartaoDebito.setValidade(dto.validade());
            cartaoDebito.setCodigoSeguranca(dto.codigoSeguranca());
        }

        repository.persist(pagamento);
    }

    @Override
    @Transactional
    public void completeInfo(Long id, BoletoRequestDTO dto) {
        Pagamento pagamento = repository.findById(id);

        if (!(pagamento instanceof Boleto)) {
            throw new IllegalStateException(
                    "O pagamento não é um boleto.");
        }

        Boleto boleto = (Boleto) pagamento;
        boleto.setCodigoBarras(dto.codigoBarras());

        repository.persist(pagamento);
    }

    @Override
    @Transactional
    public void completeInfo(Long id, PixRequestDTO dto) {
        Pagamento pagamento = repository.findById(id);

        if (!(pagamento instanceof Pix)) {
            throw new IllegalStateException(
                    "O pagamento não é um pix.");
        }

        Pix pix = (Pix) pagamento;
        pix.setChavePix(dto.chavePix());

        repository.persist(pagamento);
    }

    @Override
    @Transactional
    public void setStatus(Long id, PagamentoPatchDTO dto) {
        Pagamento pagamento = repository.findById(id);

        if (!Objects.equals(pagamento.getVersion(), dto.version())) {
            throw new OptimisticLockException(
                    "Conflito de concorrência: o pagamento foi alterado por outra transação.");
        }

        pagamento.setStatusPagamento(dto.statusPagamento());
        repository.persist(pagamento);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Valida se todos os dados obrigatórios do pagamento foram preenchidos
     * de acordo com seu tipo (Cartão, Boleto, PIX)
     */
    private void validarDadosPagamento(Pagamento pagamento) throws ValidationException {
        if (pagamento instanceof CartaoCredito) {
            validarCartao((CartaoCredito) pagamento, "crédito");
        } else if (pagamento instanceof CartaoDebito) {
            validarCartao((CartaoDebito) pagamento, "débito");
        } else if (pagamento instanceof Boleto) {
            validarBoleto((Boleto) pagamento);
        } else if (pagamento instanceof Pix) {
            validarPix((Pix) pagamento);
        } else {
            cupomService.incrementarQuantidade(pagamento.getPedido().getCupomDesconto());
            throw new ValidationException("Tipo de pagamento desconhecido.", "tipoPagamento");
        }
    }

    /**
     * Valida dados do cartão (crédito ou débito)
     */
    private void validarCartao(CartaoCredito cartao, String tipo) {
        if (cartao.getNumero() == null || cartao.getNumero().isBlank()) {
            throw new ValidationException("Número do cartão de " + tipo + " é obrigatório.", "cartao.numero");
        }
        if (cartao.getTitular() == null || cartao.getTitular().isBlank()) {
            throw new ValidationException("Nome do titular do cartão de " + tipo + " é obrigatório.", "cartao.titular");
        }
        if (cartao.getValidade() == null || cartao.getValidade().toString().isBlank()) {
            throw new ValidationException("Validade do cartão de " + tipo + " é obrigatória.", "cartao.validade");
        }
        if (cartao.getCodigoSeguranca() == null || cartao.getCodigoSeguranca().isBlank()) {
            throw new ValidationException("Código de segurança do cartão de " + tipo + " é obrigatório.",
                    "cartao.codigoSeguranca");
        }
    }

    /**
     * Valida dados do cartão débito
     */
    private void validarCartao(CartaoDebito cartao, String tipo) {
        if (cartao.getNumero() == null || cartao.getNumero().isBlank()) {
            throw new ValidationException("Número do cartão de " + tipo + " é obrigatório.", "cartao.numero");
        }
        if (cartao.getTitular() == null || cartao.getTitular().isBlank()) {
            throw new ValidationException("Nome do titular do cartão de " + tipo + " é obrigatório.", "cartao.titular");
        }
        if (cartao.getValidade() == null || cartao.getValidade().toString().isBlank()) {
            throw new ValidationException("Validade do cartão de " + tipo + " é obrigatória.", "cartao.validade");
        }
        if (cartao.getCodigoSeguranca() == null || cartao.getCodigoSeguranca().isBlank()) {
            throw new ValidationException("Código de segurança do cartão de " + tipo + " é obrigatório.",
                    "cartao.codigoSeguranca");
        }
    }

    /**
     * Valida dados do boleto
     */
    private void validarBoleto(Boleto boleto) {
        if (boleto.getCodigoBarras() == null || boleto.getCodigoBarras().isBlank()) {
            throw new ValidationException("Código de barras do boleto é obrigatório.", "boleto.codigoBarras");
        }
    }

    /**
     * Valida dados do PIX
     */
    private void validarPix(Pix pix) {
        if (pix.getChavePix() == null || pix.getChavePix().isBlank()) {
            throw new ValidationException("Chave PIX é obrigatória.", "pix.chavePix");
        }
    }

    /**
     * Remove os itens do pedido que estão na lista de desejo do usuário
     * após o pagamento ser aprovado
     */
    private void removerItensListaDesejo(Pagamento pagamento) {
        // Validar se o pagamento e pedido existem
        if (pagamento.getPedido() == null || pagamento.getPedido().getItens() == null ||
                pagamento.getPedido().getItens().isEmpty()) {
            return;
        }

        Usuario usuario = pagamento.getPedido().getUsuario();
        if (usuario == null || usuario.getLogin() == null) {
            return;
        }

        ListaDesejo listaDesejo = null;
        // Buscar a lista de desejo do usuário
        try {
            listaDesejo = listaDesejoService.findByUsuarioLogin(usuario.getLogin());
        } catch (Exception e) {
            if (listaDesejo == null || listaDesejo.getProdutos() == null || listaDesejo.getProdutos().isEmpty()) {
                return;
            }
        }

        // Obter os IDs dos produtos do pedido
        List<Long> produtoIds = pagamento.getPedido().getItens().stream()
                .map(item -> item.getPanela().getId())
                .toList();

        // Remover cada produto que está na lista de desejo
        for (Long produtoId : produtoIds) {
            listaDesejoService.removerProduto(pagamento.getPedido().getUsuario().getLogin(), produtoId);
        }
    }

    private boolean isItensPriceRight(Pagamento pagamento) {
        if (pagamento == null || pagamento.getPedido() == null || pagamento.getValor() == null) {
            return false;
        }

        BigDecimal valorCalculadoDoPedido = pagamento.getPedido().getValorBruto();

        if (pagamento.getPedido().getValorDesconto() != null) {
            valorCalculadoDoPedido = valorCalculadoDoPedido.subtract(pagamento.getPedido().getValorDesconto());
        }

        return pagamento.getValor().compareTo(valorCalculadoDoPedido) == 0;
    }

    private void atualizarCupom(CupomDesconto cupom, boolean havePagamentoBeenProcessed) {
        if (havePagamentoBeenProcessed) {
            if (cupom.getQuantidadeDisponivel() == 0)
                cupom.setAtivo(false);
        } else
            cupom.setQuantidadeDisponivel(cupom.getQuantidadeDisponivel() + 1);
    }
}
