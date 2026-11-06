package leepans.service.pagamento.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import leepans.exception.AuthorizationException;
import leepans.exception.BadRequestException;
import leepans.model.MetodoPagamento;

class GatewayFakeTest {

    private final GatewayFake gateway = new GatewayFake("segredo", 30, 3, new ObjectMapper());

    @Test
    void cartaoAprovado() {
        ResultadoCobranca resultado = gateway.criarCobranca(cartao("tok_ok", 3));

        assertEquals(StatusGateway.PAGO, resultado.status());
        assertTrue(resultado.gatewayPaymentId().startsWith("fake_"));
        assertEquals("VISA", resultado.cartao().getBandeira());
        assertEquals("4242", resultado.cartao().getUltimos4Digitos());
        assertEquals(3, resultado.cartao().getParcelas());
        assertNotNull(resultado.cartao().getCodigoAutorizacao());
    }

    @Test
    void cartaoRecusado() {
        ResultadoCobranca resultado = gateway.criarCobranca(cartao("tok_recusado_saldo", 1));

        assertEquals(StatusGateway.RECUSADO, resultado.status());
        assertEquals("cc_rejected_insufficient_amount", resultado.codigoRetorno());
    }

    @Test
    void cartaoComErroTecnico() {
        assertThrows(GatewayIndisponivelException.class, () -> gateway.criarCobranca(cartao("tok_erro", 1)));
    }

    @Test
    void payloadNaoContemTokenEConverteParaCentavos() {
        ResultadoCobranca resultado = gateway.criarCobranca(cartao("tok_ok_segredo", 1));

        assertTrue(resultado.payloadBruto().contains("\"amount\":12345"));
        assertTrue(!resultado.payloadBruto().contains("tok_ok_segredo"));
    }

    @Test
    void pix() {
        ResultadoCobranca resultado = gateway.criarCobranca(solicitacao(MetodoPagamento.PIX, null, 1));

        assertEquals(StatusGateway.AGUARDANDO_PAGAMENTO, resultado.status());
        assertEquals("00020126...FAKE" + resultado.pix().getTxid(), resultado.pix().getQrCodeCopiaECola());
        assertTrue(resultado.expiraEm().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(resultado.expiraEm().isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void boleto() {
        ResultadoCobranca resultado = gateway.criarCobranca(solicitacao(MetodoPagamento.BOLETO, null, 1));

        assertEquals(StatusGateway.AGUARDANDO_PAGAMENTO, resultado.status());
        assertTrue(resultado.boleto().getLinhaDigitavel().matches("\\d{47}"));
        assertNotNull(resultado.boleto().getUrlPdf());
        assertEquals(LocalDate.now().plusDays(3), resultado.boleto().getDataVencimento());
    }

    @Test
    void consultar_refleteOEstadoDaCobranca() {
        String id = gateway.criarCobranca(solicitacao(MetodoPagamento.PIX, null, 1)).gatewayPaymentId();
        assertEquals(StatusGateway.AGUARDANDO_PAGAMENTO, gateway.consultar(id).status());

        EventoGateway evento = gateway.simularPagamento(id);

        assertEquals(id, evento.gatewayPaymentId());
        assertEquals(StatusGateway.PAGO, evento.novoStatus());
        assertEquals(StatusGateway.PAGO, gateway.consultar(id).status());
    }

    @Test
    void cancelarEEstornar_atualizamOEstado() {
        String id = gateway.criarCobranca(solicitacao(MetodoPagamento.PIX, null, 1)).gatewayPaymentId();

        gateway.cancelar(id);
        assertEquals(StatusGateway.CANCELADO, gateway.consultar(id).status());

        assertTrue(gateway.estornar(id, BigDecimal.TEN).sucesso());
        assertEquals(StatusGateway.ESTORNADO, gateway.consultar(id).status());
    }

    @Test
    void webhook_comAssinaturaValida() {
        String corpo = "{\"id\":\"evt_1\",\"paymentId\":\"fake_1\",\"status\":\"PAGO\"}";

        EventoGateway evento = gateway.interpretarWebhook(corpo, Map.of("X-Signature", gateway.assinar(corpo)));

        assertEquals("evt_1", evento.eventoId());
        assertEquals("fake_1", evento.gatewayPaymentId());
        assertEquals(StatusGateway.PAGO, evento.novoStatus());
        assertEquals(StatusGateway.PAGO, gateway.consultar("fake_1").status());
    }

    @Test
    void webhook_assinaturaBateComOpenssl() {
        // echo -n 'corpo' | openssl dgst -sha256 -hmac segredo
        assertEquals("e91230a484e9425f85f8375e6a21b2a67662dbb11fb323f4e6be17fd290e2b5f", gateway.assinar("corpo"));
    }

    @Test
    void webhook_comAssinaturaInvalida() {
        String corpo = "{\"id\":\"evt_1\",\"paymentId\":\"fake_1\",\"status\":\"PAGO\"}";

        assertThrows(AuthorizationException.class,
                () -> gateway.interpretarWebhook(corpo, Map.of("X-Signature", "invalida")));
        assertEquals(StatusGateway.AGUARDANDO_PAGAMENTO, gateway.consultar("fake_1").status());
    }

    @Test
    void webhook_semAssinatura() {
        assertThrows(AuthorizationException.class, () -> gateway.interpretarWebhook("{}", Map.of()));
    }

    @Test
    void webhook_assinadoComCorpoMalformado() {
        String corpo = "{\"id\":\"evt_1\",\"status\":\"INEXISTENTE\"}";

        assertThrows(BadRequestException.class,
                () -> gateway.interpretarWebhook(corpo, Map.of("X-Signature", gateway.assinar(corpo))));
    }

    private static SolicitacaoCobranca cartao(String token, int parcelas) {
        return solicitacao(MetodoPagamento.CARTAO_CREDITO, token, parcelas);
    }

    private static SolicitacaoCobranca solicitacao(MetodoPagamento metodo, String token, int parcelas) {
        return new SolicitacaoCobranca(1L, metodo, new BigDecimal("123.45"), parcelas, token, "chave", "Pedido 1",
                new DadosPagador("Cliente", "cliente@test.com", null), null);
    }
}
