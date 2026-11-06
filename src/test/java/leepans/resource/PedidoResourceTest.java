package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import leepans.exception.BusinessRuleViolationException;
import leepans.exception.ResourceNotFoundException;
import leepans.model.DadosPix;
import leepans.model.EnderecoEntrega;
import leepans.model.MetodoPagamento;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPedido;
import leepans.model.Usuario;
import leepans.service.ecommerce.PagamentoService;
import leepans.service.ecommerce.PedidoService;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;
import leepans.support.TestJwt;

@QuarkusTest
class PedidoResourceTest {

    private static final String BASE = "/pedidos";
    private static final String ADMIN = BASE + "/admin";

    @InjectMock
    PedidoService service;

    @InjectMock
    PagamentoService pagamentoService;

    @BeforeEach
    void setUp() {
        reset(service, pagamentoService);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listarAdmin_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorIdAdmin_deveRetornar200() {
        when(service.findById(1L)).thenReturn(pedido(1L));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("id", equalTo(1));
    }

    @Test
    @TestJwt
    void listarCompras_deveRetornar200() {
        when(service.findCompras(TestJwt.LOGIN)).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(BASE + "/compras/me").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestJwt
    void listarComprasPorStatus_deveRetornar200() {
        when(service.findCompras(eq(TestJwt.LOGIN), eq(StatusPedido.AGUARDANDO_PAGAMENTO))).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(BASE + "/compras/me/status/AGUARDANDO_PAGAMENTO").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorUsuario_deveRetornar200() {
        when(service.findByUsuarioId(1L)).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/usuarios/1").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorStatus_deveRetornar200() {
        when(service.findByStatus(StatusPedido.AGUARDANDO_PAGAMENTO)).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/status/AGUARDANDO_PAGAMENTO").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorCidade_deveRetornar200() {
        when(service.findByEnderecoCidade("Palmas")).thenReturn(List.of(pedido(1L)));

        given()
                .accept(ContentType.JSON)
                .queryParam("cidade", "Palmas")
                .when()
                .get(ADMIN + "/enderecos/cidades")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorCidade_deveRetornar422SemParametro() {
        given().accept(ContentType.JSON).when().get(ADMIN + "/enderecos/cidades").then().statusCode(422);
    }

    @Test
    @TestJwt
    void criar_deveRetornar201() {
        when(service.create(any(), eq(TestJwt.LOGIN), any(), any())).thenReturn(pedido(5L));

        given()
                .contentType(ContentType.JSON)
                .body(pedidoPayload())
                .when()
                .post(BASE)
                .then()
                .statusCode(201)
                .body("id", equalTo(5));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void remover_deveRetornar204() {
        doNothing().when(service).delete(1L);

        given().when().delete(BASE + "/admin/1").then().statusCode(204);
    }

    @Test
    void listarCompras_deveRetornar401SemAutenticacao() {
        given().when().get(BASE + "/compras/me").then().statusCode(401);
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar201ComDadosDoPix() {
        Pagamento pagamento = pixAguardando(9L);
        when(pagamentoService.existeIdempotencyKey("chave-1")).thenReturn(false);
        when(pagamentoService.iniciar(eq(1L), eq(TestJwt.LOGIN), any(), eq("chave-1"))).thenReturn(pagamento);

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(201)
                .body("id", equalTo(9))
                .body("status", equalTo("AGUARDANDO_PAGAMENTO"))
                .body("valor", equalTo(100))
                .body("expiraEm", notNullValue())
                .body("pix.qrCodeCopiaECola", equalTo("copia-e-cola"))
                .body("pix.qrCodeBase64", equalTo("base64"))
                .body("cartao", nullValue())
                .body("boleto", nullValue());
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar200ComChaveRepetida() {
        when(pagamentoService.existeIdempotencyKey("chave-1")).thenReturn(true);
        when(pagamentoService.iniciar(eq(1L), eq(TestJwt.LOGIN), any(), eq("chave-1"))).thenReturn(pixAguardando(9L));

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(200)
                .body("id", equalTo(9));
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar422SemIdempotencyKey() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(422);

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar422SemMetodo() {
        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(422);
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar404ParaPedidoDeOutroUsuario() {
        when(pagamentoService.iniciar(eq(1L), eq(TestJwt.LOGIN), any(), eq("chave-1")))
                .thenThrow(new ResourceNotFoundException("Pedido", 1L));

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(404);
    }

    @Test
    @TestJwt
    void iniciarPagamento_deveRetornar503ComGatewayIndisponivel() {
        when(pagamentoService.iniciar(eq(1L), eq(TestJwt.LOGIN), any(), eq("chave-1")))
                .thenThrow(new GatewayIndisponivelException("fora do ar"));

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{\"metodo\":\"CARTAO_CREDITO\",\"tokenCartao\":\"tok_erro\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(503);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void iniciarPagamento_deveRetornar403ParaFuncionario() {
        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "chave-1")
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post(BASE + "/1/pagamentos")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void transicoesAdministrativas_deveRetornar204() {
        given().when().post(ADMIN + "/1/separacao").then().statusCode(204);
        given().when().post(ADMIN + "/1/envio").then().statusCode(204);
        given().when().post(ADMIN + "/1/entrega").then().statusCode(204);
        given().when().post(ADMIN + "/1/cancelamento").then().statusCode(204);

        verify(service).iniciarSeparacao(1L);
        verify(service).marcarComoEnviado(1L);
        verify(service).marcarComoEntregue(1L);
        verify(service).cancelar(1L);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void transicaoInvalida_deveRetornar422() {
        doThrow(new BusinessRuleViolationException("inválida")).when(service).marcarComoEnviado(1L);

        given().when().post(ADMIN + "/1/envio").then().statusCode(422);
    }

    @Test
    @TestJwt
    void transicoesAdministrativas_deveRetornar403ParaCliente() {
        given().when().post(ADMIN + "/1/cancelamento").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void patchDeStatus_naoExisteMais() {
        given().when().patch(ADMIN + "/1/status/ENTREGUE").then().statusCode(404);
    }

    private static Pagamento pixAguardando(Long id) {
        Pagamento pagamento = new Pagamento(pedido(1L), MetodoPagamento.PIX, new BigDecimal(100), "fake", "chave-1");
        pagamento.setId(id);
        pagamento.registrarCobranca("fake_1", null, new DadosPix("copia-e-cola", "base64", "txid"), null);
        pagamento.aguardarPagamento(LocalDateTime.now().plusMinutes(30));
        return pagamento;
    }

    private static String pedidoPayload() {
        return """
                {
                  "itens":[{"idPanela":1,"quantidade":1}],
                  "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cep":"77000-000","idCidade":1}
                }
                """;
    }

    private static Pedido pedido(Long id) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setValorItens(new BigDecimal(100));
        p.setValorDesconto(BigDecimal.ZERO);

        Usuario u = new Usuario();
        u.setId(1L);
        u.setLogin(TestJwt.LOGIN);
        p.setUsuario(u);

        p.setEnderecoEntrega(new EnderecoEntrega("Rua A", "10", null, "Centro", "Palmas", "TO", "77000-000"));

        return p;
    }
}
