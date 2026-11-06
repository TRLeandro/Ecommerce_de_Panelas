package leepans.service.ecommerce;

import java.util.List;

import leepans.dto.pagamento.IniciarPagamentoDTO;
import leepans.model.Pagamento;
import leepans.model.StatusPagamento;
import leepans.model.TransacaoGateway;
import leepans.service.pagamento.gateway.EventoGateway;

public interface PagamentoServiceInter {

    List<Pagamento> findAll();

    Pagamento findById(Long id);

    Pagamento findByIdDoUsuario(Long id, String login);

    List<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento);

    List<Pagamento> findByUsuario(String login);

    List<TransacaoGateway> findTransacoes(Long pagamentoId);

    boolean existeIdempotencyKey(String idempotencyKey);

    Pagamento iniciar(Long pedidoId, String login, IniciarPagamentoDTO dto, String idempotencyKey);

    void processarEventoGateway(EventoGateway evento, String gateway);

    void simularPagamento(Long pagamentoId);

    void estornar(Long pagamentoId);

    void cancelarNoGateway(Pagamento pagamento);

    int expirarPagamentosVencidos();
}
