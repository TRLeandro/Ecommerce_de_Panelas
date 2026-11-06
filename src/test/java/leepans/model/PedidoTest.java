package leepans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import leepans.exception.BusinessRuleViolationException;

class PedidoTest {

    private final Pedido pedido = new Pedido();

    @Test
    void valorTotal_descontaCupomESomaFrete() {
        pedido.setValorItens(new BigDecimal("100.00"));
        pedido.setValorDesconto(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("90.00"), pedido.valorTotal());
    }

    @Test
    void valorTotal_aceitaDescontoNulo() {
        pedido.setValorItens(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("100.00"), pedido.valorTotal());
    }

    @Test
    void cicloCompleto() {
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        pedido.marcarComoPago();
        pedido.iniciarSeparacao();
        pedido.marcarComoEnviado();
        pedido.marcarComoEntregue();
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
    }

    @Test
    void naoPulaEtapas() {
        assertThrows(BusinessRuleViolationException.class, pedido::iniciarSeparacao);
        assertThrows(BusinessRuleViolationException.class, pedido::marcarComoEnviado);
        assertThrows(BusinessRuleViolationException.class, pedido::marcarComoEntregue);
    }

    @Test
    void cancelar_permitidoAteASeparacao() {
        pedido.marcarComoPago();
        pedido.iniciarSeparacao();
        assertTrue(pedido.podeCancelar());

        pedido.cancelar();

        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    void cancelar_proibidoAposEnvio() {
        pedido.marcarComoPago();
        pedido.iniciarSeparacao();
        pedido.marcarComoEnviado();

        assertFalse(pedido.podeCancelar());
        assertThrows(BusinessRuleViolationException.class, pedido::cancelar);
    }

    @Test
    void cancelar_jaCancelado_lancaExcecao() {
        pedido.cancelar();

        assertThrows(BusinessRuleViolationException.class, pedido::cancelar);
    }

    @Test
    void pagamentoAtivoEAprovado() {
        Pagamento recusado = new Pagamento(pedido, MetodoPagamento.PIX, BigDecimal.TEN, "fake", "a");
        recusado.recusar("x");
        Pagamento ativo = new Pagamento(pedido, MetodoPagamento.PIX, BigDecimal.TEN, "fake", "b");
        ativo.aguardarPagamento(LocalDateTime.now());
        pedido.getPagamentos().add(recusado);
        pedido.getPagamentos().add(ativo);

        assertEquals(ativo, pedido.pagamentoAtivo().orElseThrow());
        assertTrue(pedido.pagamentoAprovado().isEmpty());

        ativo.confirmar(LocalDateTime.now());

        assertTrue(pedido.pagamentoAtivo().isEmpty());
        assertEquals(ativo, pedido.pagamentoAprovado().orElseThrow());
    }
}
