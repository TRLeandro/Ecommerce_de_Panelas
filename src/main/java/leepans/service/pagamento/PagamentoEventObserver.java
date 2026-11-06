package leepans.service.pagamento;

import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import leepans.model.CupomDesconto;
import leepans.model.ItemPedido;
import leepans.model.Pedido;
import leepans.model.Usuario;
import leepans.repository.PedidoRepository;
import leepans.service.auth.EmailService;
import leepans.service.ecommerce.CupomDescontoService;
import leepans.service.ecommerce.ListaDesejoService;
import leepans.service.pagamento.evento.PagamentoConfirmadoEvent;
import leepans.service.pagamento.evento.PagamentoExpiradoEvent;
import leepans.service.pagamento.evento.PagamentoRecusadoEvent;
import leepans.service.pagamento.evento.PedidoCanceladoEvent;
import leepans.service.pagamento.evento.PedidoCriadoEvent;

/**
 * Efeitos colaterais que só podem acontecer depois do commit. A transação que
 * disparou o evento já terminou, então todo acesso ao banco abre uma nova. Uma
 * falha aqui é logada e não desfaz o que já foi confirmado.
 */
@ApplicationScoped
public class PagamentoEventObserver {

    private static final Logger LOG = Logger.getLogger(PagamentoEventObserver.class);

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    EmailService emailService;

    @Inject
    ListaDesejoService listaDesejoService;

    @Inject
    CupomDescontoService cupomService;

    void aoCriarPedido(@Observes(during = TransactionPhase.AFTER_SUCCESS) PedidoCriadoEvent evento) {
        executar("enviar o e-mail de pedido recebido", evento.pedidoId(),
                () -> emailService.sendOrderConfirmedEmail(nomeDoCliente(evento.pedidoId()), evento.pedidoId().toString()));
    }

    void aoConfirmarPagamento(@Observes(during = TransactionPhase.AFTER_SUCCESS) PagamentoConfirmadoEvent evento) {
        Long pedidoId = evento.pedidoId();
        executar("enviar o e-mail de pagamento aprovado", pedidoId,
                () -> emailService.sendPaymentApprovedEmail(nomeDoCliente(pedidoId), pedidoId.toString()));
        executar("remover itens da lista de desejos", pedidoId,
                () -> QuarkusTransaction.requiringNew().run(() -> removerItensListaDesejo(pedidoId)));
        executar("desativar o cupom esgotado", pedidoId,
                () -> QuarkusTransaction.requiringNew().run(() -> desativarCupomEsgotado(pedidoId)));
    }

    void aoRecusarPagamento(@Observes(during = TransactionPhase.AFTER_SUCCESS) PagamentoRecusadoEvent evento) {
        String motivo = evento.motivo() != null ? evento.motivo() : "Pagamento recusado pelo gateway.";
        executar("enviar o e-mail de pagamento recusado", evento.pedidoId(), () -> emailService
                .sendPaymentRefusedEmail(nomeDoCliente(evento.pedidoId()), evento.pedidoId().toString(), motivo));
    }

    void aoExpirarPagamento(@Observes(during = TransactionPhase.AFTER_SUCCESS) PagamentoExpiradoEvent evento) {
        executar("enviar o e-mail de pagamento expirado", evento.pedidoId(), () -> emailService
                .sendPaymentRefusedEmail(nomeDoCliente(evento.pedidoId()), evento.pedidoId().toString(),
                        "O prazo para pagamento expirou."));
    }

    void aoCancelarPedido(@Observes(during = TransactionPhase.AFTER_SUCCESS) PedidoCanceladoEvent evento) {
        executar("enviar o e-mail de pedido cancelado", evento.pedidoId(), () -> emailService
                .sendOrderDeclinedEmail(nomeDoCliente(evento.pedidoId()), evento.pedidoId().toString(), evento.motivo()));
    }

    private void executar(String acao, Long pedidoId, Runnable efeito) {
        try {
            efeito.run();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao %s (pedido %d).", acao, pedidoId);
        }
    }

    private String nomeDoCliente(Long pedidoId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Usuario usuario = pedidoRepository.findById(pedidoId).getUsuario();
            return usuario.getNome() != null ? usuario.getNome() : usuario.getLogin();
        });
    }

    private void removerItensListaDesejo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId);
        String login = pedido.getUsuario().getLogin();
        for (ItemPedido item : pedido.getItens()) {
            listaDesejoService.removerProduto(login, item.getPanela().getId());
        }
    }

    private void desativarCupomEsgotado(Long pedidoId) {
        CupomDesconto cupom = pedidoRepository.findById(pedidoId).getCupomDesconto();
        if (cupom != null) {
            cupomService.desativarSeEsgotado(cupom.getId());
        }
    }
}
