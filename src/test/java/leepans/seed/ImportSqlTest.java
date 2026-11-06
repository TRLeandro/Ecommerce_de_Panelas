package leepans.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import leepans.model.ItemPedido;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;
import leepans.repository.PedidoRepository;

/**
 * O perfil de teste usa generation=update, que não executa o import.sql. Este
 * teste sobe um banco próprio com drop-and-create para carregar o seed de verdade.
 */
@QuarkusTest
@TestProfile(ImportSqlTest.Perfil.class)
class ImportSqlTest {

    public static class Perfil implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.hibernate-orm.database.generation", "drop-and-create",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:mem:panelas_seed;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        }
    }

    @Inject
    PedidoRepository pedidoRepository;

    @Test
    @Transactional
    void pedidoAguardandoTemPixAtivo() {
        Pedido pedido = pedidoRepository.findById(1L);

        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        assertEquals("São Paulo", pedido.getEnderecoEntrega().getCidade());

        Pagamento pagamento = pedido.pagamentoAtivo().orElseThrow();
        assertEquals(MetodoPagamento.PIX, pagamento.getMetodo());
        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, pagamento.getStatus());
        assertNotNull(pagamento.getPix());
    }

    @Test
    @Transactional
    void pedidoEntregueTemPagamentoPago() {
        Pedido pedido = pedidoRepository.findById(2L);

        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        assertEquals("4242", pedido.pagamentoAprovado().orElseThrow().getCartao().getUltimos4Digitos());
    }

    @Test
    @Transactional
    void totaisDosPedidosBatemComOsItens() {
        for (Pedido pedido : pedidoRepository.listAll()) {
            BigDecimal itens = BigDecimal.ZERO;
            for (ItemPedido item : pedido.getItens()) {
                assertEquals(0, item.getValorUnitario().compareTo(item.getPanela().getPreco()));
                itens = itens.add(item.getValorUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
            }
            assertEquals(0, itens.compareTo(pedido.getValorItens()), "pedido " + pedido.getId());
        }
        assertTrue(pedidoRepository.count() >= 3);
    }
}
