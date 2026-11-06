package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
import leepans.dto.pagamento.BoletoRequestDTO;
import leepans.dto.pagamento.CartaoRequestDTO;
import leepans.dto.pagamento.PixRequestDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.Pix;
import leepans.model.StatusPagamento;
import leepans.model.TipoPagamento;
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
    void processar_deveRetornar204() {
        Pagamento pagamento = pagamento(1L);
        when(service.findById(1L)).thenReturn(pagamento);
        doNothing().when(service).processarPagamento(pagamento);

        given().when().put(BASE + "/admin/processar/1").then().statusCode(204);
    }

    @Test
    @TestJwt
    void completarCartao_deveRetornar204() {
        doNothing().when(service).completeInfo(eq(1L), any(CartaoRequestDTO.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "numero":"4111111111111111",
                          "titular":"Titular Teste",
                          "validade":"2030-12-31",
                          "codigoSeguranca":"123",
                          "isCredito":true
                        }
                        """)
                .when()
                .put(BASE + "/cartao/1")
                .then()
                .statusCode(204);
    }

    @Test
    @TestJwt
    void completarCartao_deveRetornar404ParaPagamentoDeOutroUsuario() {
        when(service.findByIdDoUsuario(1L, TestJwt.LOGIN)).thenThrow(new ResourceNotFoundException("Pagamento", 1L));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "numero":"4111111111111111",
                          "titular":"Titular Teste",
                          "validade":"2030-12-31",
                          "codigoSeguranca":"123",
                          "isCredito":true
                        }
                        """)
                .when()
                .put(BASE + "/cartao/1")
                .then()
                .statusCode(404);

        verify(service, never()).completeInfo(eq(1L), any(CartaoRequestDTO.class));
    }

    @Test
    @TestJwt
    void completarBoleto_deveRetornar204() {
        doNothing().when(service).completeInfo(eq(1L), any(BoletoRequestDTO.class));

        given()
                .contentType(ContentType.JSON)
                .body("{\"codigoBarras\":\"12345678901234567890123456789012345678901234567\"}")
                .when()
                .put(BASE + "/boleto/1")
                .then()
                .statusCode(204);
    }

    @Test
    @TestJwt
    void completarPix_deveRetornar204() {
        doNothing().when(service).completeInfo(eq(1L), any(PixRequestDTO.class));

        given()
                .contentType(ContentType.JSON)
                .body("{\"chavePix\":\"cliente@test.com\"}")
                .when()
                .put(BASE + "/pix/1")
                .then()
                .statusCode(204);
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
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizarStatus_deveRetornar204() {
        doNothing().when(service).setStatus(any(Long.class), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"statusPagamento\":\"APROVADO\",\"version\":1}")
                .when()
                .patch(ADMIN + "/1")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void remover_deveRetornar204() {
        doNothing().when(service).delete(1L);

        given().when().delete(ADMIN + "/1").then().statusCode(204);
    }

    private static Pagamento pagamento(Long id) {
        Pix p = new Pix();
        p.setId(id);
        p.setStatusPagamento(StatusPagamento.PENDENTE);
        p.setTipoPagamento(TipoPagamento.PIX);
        p.setValor(new BigDecimal(100));
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        p.setPedido(pedido);
        return p;
    }
}
