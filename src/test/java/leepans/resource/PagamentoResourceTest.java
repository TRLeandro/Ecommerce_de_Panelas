package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import leepans.exception.ResourceNotFoundException;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPagamento;
import leepans.model.TipoTransacao;
import leepans.model.TransacaoGateway;
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

    @Test
    @TestJwt
    void buscarMeuPagamento_deveRetornar200() {
        when(service.findByIdDoUsuario(1L, TestJwt.LOGIN)).thenReturn(pagamento(1L));

        given().accept(ContentType.JSON).when().get(BASE + "/1").then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("status", equalTo("PENDENTE"))
                .body("metodo", equalTo("PIX"));
    }

    @Test
    @TestJwt
    void buscarMeuPagamento_deveRetornar404ParaPagamentoDeOutroUsuario() {
        when(service.findByIdDoUsuario(1L, TestJwt.LOGIN)).thenThrow(new ResourceNotFoundException("Pagamento", 1L));

        given().accept(ContentType.JSON).when().get(BASE + "/1").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listarTransacoes_deveRetornar200() {
        Pagamento pagamento = pagamento(1L);
        when(service.findTransacoes(1L)).thenReturn(List.of(new TransacaoGateway(pagamento, TipoTransacao.CRIACAO,
                "PAGO", "approved", "ok", "{}")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1/transacoes").then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].tipo", equalTo("CRIACAO"))
                .body("[0].statusGateway", equalTo("PAGO"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void estornar_deveRetornar200() {
        when(service.findById(1L)).thenReturn(pagamento(1L));

        given().when().post(ADMIN + "/1/estorno").then().statusCode(200).body("id", equalTo(1));

        verify(service).estornar(1L);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void estornar_deveRetornar403ParaFuncionario() {
        given().when().post(ADMIN + "/1/estorno").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void simularPagamento_deveRetornar200() {
        when(service.findById(1L)).thenReturn(pagamento(1L));

        given().when().post(ADMIN + "/1/simular-pagamento").then().statusCode(200);

        verify(service).simularPagamento(1L);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void simularPagamento_deveRetornar404SemGatewayFake() {
        doThrow(new ResourceNotFoundException("A simulação só existe com o gateway fake."))
                .when(service).simularPagamento(1L);

        given().when().post(ADMIN + "/1/simular-pagamento").then().statusCode(404);
    }

    @Test
    @TestJwt
    void rotasAdministrativas_deveRetornar403ParaCliente() {
        given().when().get(ADMIN).then().statusCode(403);
        given().when().post(ADMIN + "/1/simular-pagamento").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void endpointsRemovidos_naoExistemMais() {
        given().contentType(ContentType.JSON).body("{}").when().put(BASE + "/pix/1").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").when().put(BASE + "/cartao/1").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").when().put(BASE + "/boleto/1").then().statusCode(404);
        given().when().put(ADMIN + "/processar/1").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").when().patch(ADMIN + "/1").then().statusCode(405);
        given().when().delete(ADMIN + "/1").then().statusCode(405);
    }

    private static Pagamento pagamento(Long id) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        Pagamento p = new Pagamento(pedido, MetodoPagamento.PIX, new BigDecimal(100), "fake", "chave-" + id);
        p.setId(id);
        return p;
    }
}
