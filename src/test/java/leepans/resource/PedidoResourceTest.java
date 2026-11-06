package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import leepans.model.Endereco;
import leepans.model.Pedido;
import leepans.model.StatusPedido;
import leepans.model.Usuario;
import leepans.service.ecommerce.PedidoService;
import leepans.support.TestJwt;

@QuarkusTest
class PedidoResourceTest {

    private static final String BASE = "/pedidos";
    private static final String ADMIN = BASE + "/admin";

    @InjectMock
    PedidoService service;

    @BeforeEach
    void setUp() {
        reset(service);
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
        when(service.findCompras(eq(TestJwt.LOGIN), eq(StatusPedido.PENDENTE))).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(BASE + "/compras/me/status/PENDENTE").then()
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
        when(service.findByStatus(StatusPedido.PENDENTE)).thenReturn(List.of(pedido(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/status/PENDENTE").then()
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
        when(service.create(any(), eq(TestJwt.LOGIN), any(), any(), any())).thenReturn(pedido(5L));

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
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizarStatus_deveRetornar204() {
        doNothing().when(service).setStatus(1L, StatusPedido.ENTREGUE);

        given().when().patch(ADMIN + "/1/status/ENTREGUE").then().statusCode(204);
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

    private static String pedidoPayload() {
        return """
                {
                  "itens":[{"idPanela":1,"quantidade":1}],
                  "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cep":"77000-000","idCidade":1},
                  "pagamento":"PIX"
                }
                """;
    }

    private static Pedido pedido(Long id) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setStatus(StatusPedido.PENDENTE);
        p.setValorBruto(new BigDecimal(100));
        p.setValorDesconto(BigDecimal.ZERO);

        Usuario u = new Usuario();
        u.setId(1L);
        u.setLogin(TestJwt.LOGIN);
        p.setUsuario(u);

        Endereco e = new Endereco();
        e.setId(1L);
        leepans.model.Cidade c = new leepans.model.Cidade();
        c.setId(1L);
        c.setNome("Palmas");
        e.setCidade(c);
        p.setEndereco(e);

        return p;
    }
}
