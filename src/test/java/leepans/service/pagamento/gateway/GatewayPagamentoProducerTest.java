package leepans.service.pagamento.gateway;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GatewayPagamentoProducerTest {

    private final GatewayPagamento fake = new GatewayFake("segredo", 30, 3, new ObjectMapper());

    @Test
    void selecionaPeloNome() {
        assertSame(fake, GatewayPagamentoProducer.selecionar("fake", List.of(fake)));
    }

    @Test
    void valorDesconhecido_falhaComMensagemClara() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> GatewayPagamentoProducer.selecionar("mercadopago", List.of(fake)));

        assertTrue(erro.getMessage().contains("mercadopago"));
        assertTrue(erro.getMessage().contains("fake"));
    }
}
