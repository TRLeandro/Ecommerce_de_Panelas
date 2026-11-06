package leepans.service.ecommerce;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import leepans.dto.pagamento.IniciarPagamentoDTO;
import leepans.exception.BusinessRuleViolationException;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.model.EventoWebhook;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;
import leepans.model.TipoTransacao;
import leepans.model.TransacaoGateway;
import leepans.model.Usuario;
import leepans.repository.EventoWebhookRepository;
import leepans.repository.PagamentoRepository;
import leepans.repository.PedidoRepository;
import leepans.repository.TransacaoGatewayRepository;
import leepans.service.pagamento.evento.PagamentoConfirmadoEvent;
import leepans.service.pagamento.evento.PagamentoExpiradoEvent;
import leepans.service.pagamento.evento.PagamentoRecusadoEvent;
import leepans.service.pagamento.evento.PedidoCanceladoEvent;
import leepans.service.pagamento.gateway.DadosPagador;
import leepans.service.pagamento.gateway.EstornoNegadoException;
import leepans.service.pagamento.gateway.EventoGateway;
import leepans.service.pagamento.gateway.GatewayFake;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;
import leepans.service.pagamento.gateway.GatewayPagamento;
import leepans.service.pagamento.gateway.GatewaySelecionado;
import leepans.service.pagamento.gateway.ResultadoCobranca;
import leepans.service.pagamento.gateway.ResultadoEstorno;
import leepans.service.pagamento.gateway.SolicitacaoCobranca;
import leepans.service.pagamento.gateway.StatusGateway;

/**
 * As operações que mexem em dinheiro travam a linha do pedido durante toda a
 * transação, inclusive na chamada ao gateway. Isso garante um único pagamento
 * ativo por pedido e custa uma conexão presa enquanto o gateway responde.
 */
@ApplicationScoped
public class PagamentoService implements PagamentoServiceInter {

    private static final Logger LOG = Logger.getLogger(PagamentoService.class);

    @Inject
    PagamentoRepository repository;

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    EventoWebhookRepository eventoWebhookRepository;

    @Inject
    TransacaoGatewayRepository transacaoRepository;

    @Inject
    PedidoService pedidoService;

    @Inject
    CupomDescontoService cupomService;

    @Inject
    @GatewaySelecionado
    GatewayPagamento gateway;

    @Inject
    GatewayFake gatewayFake;

    @Inject
    Event<PagamentoConfirmadoEvent> pagamentoConfirmado;

    @Inject
    Event<PagamentoRecusadoEvent> pagamentoRecusado;

    @Inject
    Event<PagamentoExpiradoEvent> pagamentoExpirado;

    @Inject
    Event<PedidoCanceladoEvent> pedidoCancelado;

    @ConfigProperty(name = "app.pagamento.cartao.max-parcelas", defaultValue = "12")
    int maxParcelas;

    @ConfigProperty(name = "app.pedido.horas-para-cancelamento", defaultValue = "24")
    int horasParaCancelamento;

    @Override
    public List<Pagamento> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Pagamento findById(Long id) {
        Pagamento pagamento = repository.findById(id);
        if (pagamento == null) {
            throw new ResourceNotFoundException("Pagamento", id);
        }
        return pagamento;
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
    public List<TransacaoGateway> findTransacoes(Long pagamentoId) {
        findById(pagamentoId);
        return transacaoRepository.findByPagamentoId(pagamentoId);
    }

    @Override
    public boolean existeIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey) != null;
    }

    /**
     * O dono é conferido antes da chave de idempotência: quem conhece a chave de
     * outro cliente não pode receber o pagamento dele.
     */
    @Override
    @Transactional(dontRollbackOn = GatewayIndisponivelException.class)
    public Pagamento iniciar(Long pedidoId, String login, IniciarPagamentoDTO dto, String idempotencyKey) {
        Pedido pedido = pedidoService.findByIdDoUsuario(pedidoId, login);
        pedidoRepository.travar(pedido);

        Pagamento existente = repository.findByIdempotencyKey(idempotencyKey);
        if (existente != null) {
            if (!existente.getPedido().getId().equals(pedidoId) || existente.getMetodo() != dto.metodo()) {
                throw new BusinessRuleViolationException(
                        "Esta Idempotency-Key já foi usada em outra operação.", "Idempotency-Key");
            }
            return existente;
        }

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new BusinessRuleViolationException(
                    "Não é possível pagar um pedido com status " + pedido.getStatus() + ".", "status");
        }

        int parcelas = validarMetodo(dto);

        pedido.pagamentoAtivo().ifPresent(this::cancelarNoGateway);

        Pagamento pagamento = new Pagamento(pedido, dto.metodo(), pedido.valorTotal(), gateway.nome(), idempotencyKey);
        repository.persist(pagamento);
        pedido.getPagamentos().add(pagamento);

        ResultadoCobranca resultado;
        try {
            resultado = gateway.criarCobranca(montarSolicitacao(pagamento, dto, parcelas));
        } catch (GatewayIndisponivelException e) {
            registrarTransacao(pagamento, TipoTransacao.CRIACAO, null, null, e.getMessage(), null);
            throw e;
        }

        pagamento.registrarCobranca(resultado.gatewayPaymentId(), resultado.cartao(), resultado.pix(), resultado.boleto());
        registrarTransacao(pagamento, TipoTransacao.CRIACAO, resultado.status().name(), resultado.codigoRetorno(),
                resultado.mensagem(), resultado.payloadBruto());
        aplicarStatusGateway(pagamento, resultado);

        return pagamento;
    }

    /** @return a quantidade de parcelas a cobrar */
    private int validarMetodo(IniciarPagamentoDTO dto) {
        if (dto.metodo().isCartao() && (dto.tokenCartao() == null || dto.tokenCartao().isBlank())) {
            throw new ValidationException("O token do cartão é obrigatório.", "tokenCartao");
        }
        if (dto.metodo() != MetodoPagamento.CARTAO_CREDITO || dto.parcelas() == null) {
            return 1;
        }
        if (dto.parcelas() < 1 || dto.parcelas() > maxParcelas) {
            throw new ValidationException("As parcelas devem ficar entre 1 e " + maxParcelas + ".", "parcelas");
        }
        return dto.parcelas();
    }

    private SolicitacaoCobranca montarSolicitacao(Pagamento pagamento, IniciarPagamentoDTO dto, int parcelas) {
        Usuario usuario = pagamento.getPedido().getUsuario();
        String nome = Stream.of(usuario.getNome(), usuario.getSobrenome())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return new SolicitacaoCobranca(
                pagamento.getId(),
                pagamento.getMetodo(),
                pagamento.getValor(),
                parcelas,
                dto.tokenCartao(),
                pagamento.getIdempotencyKey(),
                "Pedido #" + pagamento.getPedido().getId(),
                new DadosPagador(nome, usuario.getLogin(), null),
                null);
    }

    /**
     * Registrar e aplicar ficam em transações separadas, e o evento só vira
     * processado junto com a mudança de status. Se a aplicação falhar, o gateway
     * reenvia e o evento registrado, ainda não processado, é refeito.
     */
    @Override
    public void processarEventoGateway(EventoGateway evento, String nomeGateway) {
        try {
            boolean jaProcessado = QuarkusTransaction.requiringNew().call(() -> registrarEvento(evento, nomeGateway));
            if (jaProcessado) {
                return;
            }
        } catch (ConstraintViolationException e) {
            LOG.debugf("Evento %s recebido em paralelo: quem o inseriu fará o processamento.", evento.eventoId());
            return;
        }

        // A fonte da verdade é a consulta ao gateway, não o corpo do webhook
        ResultadoCobranca atual = gateway.consultar(evento.gatewayPaymentId());

        QuarkusTransaction.requiringNew().run(() -> aplicarEvento(evento, nomeGateway, atual));
    }

    private boolean registrarEvento(EventoGateway evento, String nomeGateway) {
        EventoWebhook registrado = eventoWebhookRepository.findByGatewayEEvento(nomeGateway, evento.eventoId());
        if (registrado != null) {
            return registrado.isProcessado();
        }
        eventoWebhookRepository.persist(new EventoWebhook(nomeGateway, evento.eventoId(), evento.payloadBruto()));
        return false;
    }

    private void aplicarEvento(EventoGateway evento, String nomeGateway, ResultadoCobranca atual) {
        EventoWebhook registro = eventoWebhookRepository
                .findByGatewayEEventoParaAtualizacao(nomeGateway, evento.eventoId());
        if (registro.isProcessado()) {
            return;
        }

        Pagamento pagamento = repository.findByGatewayPaymentId(evento.gatewayPaymentId());
        if (pagamento == null) {
            // Sem marcar como processado: o webhook pode ter chegado antes do commit de iniciar()
            throw new ResourceNotFoundException("Pagamento", evento.gatewayPaymentId());
        }
        travar(pagamento);

        registrarTransacao(pagamento, TipoTransacao.WEBHOOK, atual.status().name(), atual.codigoRetorno(),
                atual.mensagem(), evento.payloadBruto());
        try {
            aplicarStatusGateway(pagamento, atual);
        } catch (BusinessRuleViolationException e) {
            // Repetir não resolve (ex.: Pix pago depois de expirar). Fica no log para conciliação manual.
            LOG.errorf("Evento %s ignorado para o pagamento %d: gateway diz %s, status local %s. %s",
                    evento.eventoId(), pagamento.getId(), atual.status(), pagamento.getStatus(), e.getMessage());
        }
        registro.marcarComoProcessado();
    }

    @Override
    public void simularPagamento(Long pagamentoId) {
        if (!GatewayFake.NOME.equals(gateway.nome())) {
            throw new ResourceNotFoundException("A simulação só existe com o gateway fake.");
        }
        // Em transação própria: fora dela a entidade ficaria na sessão da requisição e quem
        // relesse o pagamento depois da simulação receberia o status antigo
        String gatewayPaymentId = QuarkusTransaction.requiringNew().call(() -> buscarCobrancaEmAberto(pagamentoId));
        processarEventoGateway(gatewayFake.simularPagamento(gatewayPaymentId), gateway.nome());
    }

    private String buscarCobrancaEmAberto(Long pagamentoId) {
        Pagamento pagamento = findById(pagamentoId);
        if (!pagamento.getStatus().isAtivo() || pagamento.getGatewayPaymentId() == null) {
            throw new BusinessRuleViolationException(
                    "Não há cobrança em aberto para simular no pagamento " + pagamentoId + ".", "status");
        }
        return pagamento.getGatewayPaymentId();
    }

    @Override
    @Transactional(dontRollbackOn = { GatewayIndisponivelException.class, EstornoNegadoException.class })
    public void estornar(Long pagamentoId) {
        Pagamento pagamento = findById(pagamentoId);
        travar(pagamento);

        // Antes de chamar o gateway: depois que o dinheiro volta, a transição não pode mais falhar
        if (pagamento.getStatus() != StatusPagamento.PAGO) {
            throw new BusinessRuleViolationException(
                    "Não é possível estornar um pagamento com status " + pagamento.getStatus() + ".", "status");
        }
        if (!pagamento.getPedido().podeCancelar()) {
            throw new BusinessRuleViolationException(
                    "Não é possível estornar: o pedido já está com status " + pagamento.getPedido().getStatus() + ".",
                    "status");
        }

        ResultadoEstorno resultado;
        try {
            resultado = gateway.estornar(pagamento.getGatewayPaymentId(), pagamento.getValor());
        } catch (GatewayIndisponivelException e) {
            registrarTransacao(pagamento, TipoTransacao.ESTORNO, null, null, e.getMessage(), null);
            throw e;
        }

        String status = resultado.sucesso() ? StatusGateway.ESTORNADO.name() : null;
        registrarTransacao(pagamento, TipoTransacao.ESTORNO, status, null, resultado.mensagem(), resultado.payloadBruto());
        if (!resultado.sucesso()) {
            throw new EstornoNegadoException("O gateway negou o estorno: " + resultado.mensagem());
        }

        aplicarEstorno(pagamento);
    }

    private void aplicarEstorno(Pagamento pagamento) {
        pagamento.estornar();
        // TODO estoque: liberar os itens do pedido
        devolverCupom(pagamento.getPedido());
        pedidoCancelado.fire(new PedidoCanceladoEvent(pagamento.getPedido().getId(), "Pagamento estornado."));
    }

    /**
     * Cancela o pagamento ativo, inclusive no gateway. Quem chama precisa estar
     * em uma transação com o pedido travado.
     */
    @Override
    public void cancelarNoGateway(Pagamento pagamento) {
        // Sem ID o gateway nunca confirmou a cobrança, e não há o que cancelar lá
        if (pagamento.getGatewayPaymentId() != null) {
            try {
                gateway.cancelar(pagamento.getGatewayPaymentId());
            } catch (GatewayIndisponivelException e) {
                registrarTransacao(pagamento, TipoTransacao.CANCELAMENTO, null, null, e.getMessage(), null);
                throw e;
            }
            registrarTransacao(pagamento, TipoTransacao.CANCELAMENTO, StatusGateway.CANCELADO.name(), null, null, null);
        }
        pagamento.cancelar();
    }

    /**
     * Cada item roda na própria transação: falha em um não impede os demais, e
     * o que falhou volta a ser examinado na próxima execução.
     */
    @Override
    public int expirarPagamentosVencidos() {
        LocalDateTime agora = LocalDateTime.now();
        int processados = 0;

        List<Long> vencidos = QuarkusTransaction.requiringNew().call(() -> repository.findIdsVencidos(agora));
        for (Long id : vencidos) {
            try {
                QuarkusTransaction.requiringNew().run(() -> expirarPagamento(id));
                processados++;
            } catch (RuntimeException e) {
                LOG.errorf(e, "Falha ao expirar o pagamento %d.", id);
            }
        }

        // Depois dos vencidos, para pegar também os pedidos que acabaram de perder o pagamento
        LocalDateTime limite = agora.minusHours(horasParaCancelamento);
        List<Long> abandonados = QuarkusTransaction.requiringNew().call(() -> pedidoRepository.findIdsAbandonados(limite));
        for (Long id : abandonados) {
            try {
                QuarkusTransaction.requiringNew().run(() -> cancelarPedidoAbandonado(id));
                processados++;
            } catch (RuntimeException e) {
                LOG.errorf(e, "Falha ao cancelar o pedido abandonado %d.", id);
            }
        }

        return processados;
    }

    private void expirarPagamento(Long id) {
        Pagamento pagamento = findById(id);
        travar(pagamento);
        if (pagamento.getStatus() != StatusPagamento.AGUARDANDO_PAGAMENTO) {
            return;
        }

        ResultadoCobranca atual = gateway.consultar(pagamento.getGatewayPaymentId());
        registrarTransacao(pagamento, TipoTransacao.CONSULTA, atual.status().name(), atual.codigoRetorno(),
                atual.mensagem(), atual.payloadBruto());

        if (atual.status() == StatusGateway.PAGO) {
            aplicarStatusGateway(pagamento, atual);
            return;
        }

        aplicarExpiracao(pagamento);
    }

    private void aplicarExpiracao(Pagamento pagamento) {
        pagamento.expirar();
        pagamentoExpirado.fire(new PagamentoExpiradoEvent(pagamento.getId(), pagamento.getPedido().getId()));
    }

    private void cancelarPedidoAbandonado(Long id) {
        Pedido pedido = pedidoRepository.findById(id);
        pedidoRepository.travar(pedido);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO || pedido.pagamentoAtivo().isPresent()) {
            return;
        }

        pedido.cancelar();
        // TODO estoque: liberar os itens do pedido
        devolverCupom(pedido);
        pedidoCancelado.fire(new PedidoCanceladoEvent(id, "Pagamento não realizado no prazo."));
    }

    private void devolverCupom(Pedido pedido) {
        if (pedido.getCupomDesconto() != null) {
            cupomService.incrementarQuantidade(pedido.getCupomDesconto());
        }
    }

    /** Trava o pedido primeiro, a mesma ordem de iniciar(), e recarrega o pagamento já sob a trava. */
    private void travar(Pagamento pagamento) {
        pedidoRepository.travar(pagamento.getPedido());
        repository.getEntityManager().refresh(pagamento);
    }

    private void registrarTransacao(Pagamento pagamento, TipoTransacao tipo, String statusGateway, String codigoRetorno,
            String mensagem, String payloadBruto) {
        pagamento.adicionarTransacao(
                new TransacaoGateway(pagamento, tipo, statusGateway, codigoRetorno, mensagem, payloadBruto));
    }

    /**
     * Traduz o status do gateway para a transição da entidade. Status igual ao
     * atual não faz nada, porque consultas e webhooks se repetem.
     */
    private void aplicarStatusGateway(Pagamento pagamento, ResultadoCobranca resultado) {
        StatusGateway status = resultado.status();
        if (status.name().equals(pagamento.getStatus().name())) {
            return;
        }

        Long pedidoId = pagamento.getPedido().getId();
        switch (status) {
            case PENDENTE -> {
                // Nada mudou no gateway
            }
            case AGUARDANDO_PAGAMENTO -> pagamento.aguardarPagamento(resultado.expiraEm());
            case AUTORIZADO -> pagamento.autorizar(
                    resultado.cartao() != null ? resultado.cartao().getCodigoAutorizacao() : null);
            case PAGO -> {
                pagamento.confirmar(LocalDateTime.now());
                pagamentoConfirmado.fire(new PagamentoConfirmadoEvent(pagamento.getId(), pedidoId));
            }
            case RECUSADO -> {
                pagamento.recusar(resultado.mensagem());
                pagamentoRecusado.fire(new PagamentoRecusadoEvent(pagamento.getId(), pedidoId, resultado.mensagem()));
            }
            case EXPIRADO -> aplicarExpiracao(pagamento);
            case CANCELADO -> pagamento.cancelar();
            case ESTORNADO -> aplicarEstorno(pagamento);
        }
    }
}
