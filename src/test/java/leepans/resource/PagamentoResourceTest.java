package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPagamento;
import leepans.service.ecommerce.PagamentoService;
import leepans.support.TestJwt;

@QuarkusTest
class PagamentoResourceTest {

    private static final String BASE = "/pagamentos";
    private static final String ADMIN = BASE + "/admin";

    @InjectMock
    PagamentoService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listarAdmin_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(pagamento(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorIdAdmin_deveRetornar200() {
        when(service.findById(1L)).thenReturn(pagamento(1L));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("id", equalTo(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorStatus_deveRetornar200() {
        when(service.findByStatusPagamento(StatusPagamento.PENDENTE)).thenReturn(List.of(pagamento(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/status/PENDENTE").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestJwt
    void listarEcommerce_deveRetornar200() {
        when(service.findByUsuario(TestJwt.LOGIN)).thenReturn(List.of(pagamento(1L)));

        given().accept(ContentType.JSON).when().get(BASE + "/ecommerce").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    private static Pagamento pagamento(Long id) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        Pagamento p = new Pagamento(pedido, MetodoPagamento.PIX, new BigDecimal(100), "fake", "chave-" + id);
        p.setId(id);
        return p;
    }
}
