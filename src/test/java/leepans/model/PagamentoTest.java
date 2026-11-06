package leepans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import leepans.exception.BusinessRuleViolationException;

class PagamentoTest {

    private final Pedido pedido = new Pedido();
    private final Pagamento pagamento = new Pagamento(pedido, MetodoPagamento.PIX, new BigDecimal("10.00"), "fake", "k");

    @Test
    void nasceComoPendente() {
        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
    }

    @Test
    void aguardarPagamento_registraExpiracao() {
        LocalDateTime expira = LocalDateTime.now().plusMinutes(30);

        pagamento.aguardarPagamento(expira);

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, pagamento.getStatus());
        assertEquals(expira, pagamento.getExpiraEm());
    }

    @Test
    void autorizar_guardaCodigo() {
        pagamento.autorizar("ABC");

        assertEquals(StatusPagamento.AUTORIZADO, pagamento.getStatus());
        assertEquals("ABC", pagamento.getCartao().getCodigoAutorizacao());
    }

    @Test
    void confirmar_marcaPedidoComoPago() {
        LocalDateTime agora = LocalDateTime.now();

        pagamento.confirmar(agora);

        assertEquals(StatusPagamento.PAGO, pagamento.getStatus());
        assertEquals(agora, pagamento.getPagoEm());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
    }

    @Test
    void confirmar_jaPago_naoFazNada() {
        LocalDateTime primeira = LocalDateTime.now();
        pagamento.confirmar(primeira);

        pagamento.confirmar(primeira.plusHours(1));

        assertEquals(primeira, pagamento.getPagoEm());
    }

    @Test
    void confirmar_comPedidoCancelado_naoMudaOPagamento() {
        pagamento.aguardarPagamento(LocalDateTime.now());
        pedido.cancelar();

        assertThrows(BusinessRuleViolationException.class, () -> pagamento.confirmar(LocalDateTime.now()));

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, pagamento.getStatus());
        assertNull(pagamento.getPagoEm());
    }

    @Test
    void recusar_guardaMotivoEMantemPedido() {
        pagamento.recusar("saldo");

        assertEquals(StatusPagamento.RECUSADO, pagamento.getStatus());
        assertEquals("saldo", pagamento.getMotivoRecusa());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
    }

    @Test
    void cancelar_registraData() {
        pagamento.cancelar();

        assertEquals(StatusPagamento.CANCELADO, pagamento.getStatus());
        assertNotNull(pagamento.getCanceladoEm());
    }

    @Test
    void estornar_cancelaPedido() {
        pagamento.confirmar(LocalDateTime.now());

        pagamento.estornar();

        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    void estornar_comPedidoEnviado_naoMudaOPagamento() {
        pagamento.confirmar(LocalDateTime.now());
        pedido.iniciarSeparacao();
        pedido.marcarComoEnviado();

        assertThrows(BusinessRuleViolationException.class, pagamento::estornar);

        assertEquals(StatusPagamento.PAGO, pagamento.getStatus());
    }

    @Test
    void contestar_mantemPedido() {
        pagamento.confirmar(LocalDateTime.now());

        pagamento.contestar();

        assertEquals(StatusPagamento.CONTESTADO, pagamento.getStatus());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
    }

    @Test
    void transicoesInvalidas_lancamExcecao() {
        assertThrows(BusinessRuleViolationException.class, pagamento::expirar);
        assertThrows(BusinessRuleViolationException.class, pagamento::estornar);
        assertThrows(BusinessRuleViolationException.class, pagamento::contestar);

        pagamento.aguardarPagamento(LocalDateTime.now());
        assertThrows(BusinessRuleViolationException.class, () -> pagamento.recusar("x"));
        assertThrows(BusinessRuleViolationException.class, () -> pagamento.autorizar("x"));

        pagamento.expirar();
        assertThrows(BusinessRuleViolationException.class, () -> pagamento.confirmar(LocalDateTime.now()));
        assertThrows(BusinessRuleViolationException.class, pagamento::cancelar);
        assertThrows(BusinessRuleViolationException.class, () -> pagamento.aguardarPagamento(LocalDateTime.now()));
    }

    @Test
    void statusAtivosEFinais() {
        assertTrue(StatusPagamento.PENDENTE.isAtivo());
        assertTrue(StatusPagamento.AGUARDANDO_PAGAMENTO.isAtivo());
        assertTrue(StatusPagamento.AUTORIZADO.isAtivo());
        assertFalse(StatusPagamento.PAGO.isAtivo());
        assertFalse(StatusPagamento.PAGO.isFinal());
        assertFalse(StatusPagamento.PENDENTE.isFinal());
        assertTrue(StatusPagamento.RECUSADO.isFinal());
        assertTrue(StatusPagamento.ESTORNADO.isFinal());
        assertTrue(StatusPagamento.CONTESTADO.isFinal());
    }

    @Test
    void transacao_mascaraTokenDeCartao() {
        TransacaoGateway transacao = new TransacaoGateway(pagamento, TipoTransacao.CRIACAO, "PAGO", null,
                "falha com tok_abc123",
                "{\"card_token\":\"segredo\",\"tokenCartao\":\"tok_xyz\",\"id\":\"fake_1\",\"ref\":\"tok_solto\"}");

        assertEquals("falha com ***", transacao.getMensagem());
        assertEquals("{\"card_token\":\"***\",\"tokenCartao\":\"***\",\"id\":\"fake_1\",\"ref\":\"***\"}",
                transacao.getPayloadBruto());
    }
}
