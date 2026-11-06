package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import leepans.model.Cidade;
import leepans.model.Estado;
import leepans.model.Panela;
import leepans.model.Perfil;
import leepans.model.Regiao;
import leepans.model.Usuario;
import leepans.repository.CidadeRepository;
import leepans.repository.EstadoRepository;
import leepans.repository.PanelaRepository;
import leepans.repository.RegiaoRepository;
import leepans.repository.UsuarioRepository;
import leepans.support.TestJwt;

/** Fluxo completo pela API, sem mocks: resource, serviço, banco e GatewayFake. */
@QuarkusTest
class PagamentoFluxoTest {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PanelaRepository panelaRepository;

    @Inject
    CidadeRepository cidadeRepository;

    @Inject
    EstadoRepository estadoRepository;

    @Inject
    RegiaoRepository regiaoRepository;

    @ConfigProperty(name = "app.pagamento.webhook-secret")
    String segredo;

    private Long panelaId;
    private Long cidadeId;

    @BeforeEach
    void preparar() {
        QuarkusTransaction.requiringNew().run(() -> {
            if (usuarioRepository.findByLogin(TestJwt.LOGIN).firstResult() == null) {
                Usuario usuario = new Usuario();
                usuario.setLogin(TestJwt.LOGIN);
                usuario.setNome("Cliente");
                usuario.setSenhaHash("hash");
                usuario.setPerfil(Perfil.CLIENTE);
                usuarioRepository.persist(usuario);
            }

            Panela panela = new Panela();
            panela.setModelo("Fluxo");
            panela.setPreco(new BigDecimal("80.00"));
            panela.setIsInducao(false);
            panelaRepository.persist(panela);
            panelaId = panela.getId();

            Regiao regiao = new Regiao();
            regiao.setNome("Norte");
            regiaoRepository.persist(regiao);
            Estado estado = new Estado();
            estado.setNome("Tocantins");
            estado.setSigla("TO");
            estado.setRegiao(regiao);
            estadoRepository.persist(estado);
            Cidade cidade = new Cidade();
            cidade.setNome("Palmas");
            cidade.setEstado(estado);
            cidadeRepository.persist(cidade);
            cidadeId = cidade.getId();
        });
    }

    @Test
    @TestJwt
    void pix_ateAConfirmacaoPorWebhook() {
        int pedidoId = criarPedido();

        var pagamento = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post("/pedidos/" + pedidoId + "/pagamentos")
                .then()
                .statusCode(201)
                .body("status", equalTo("AGUARDANDO_PAGAMENTO"))
                .body("pix.qrCodeCopiaECola", notNullValue())
                .extract().jsonPath();

        int pagamentoId = pagamento.getInt("id");
        String corpo = "{\"id\":\"evt_%s\",\"paymentId\":\"%s\",\"status\":\"PAGO\"}"
                .formatted(UUID.randomUUID(), pagamento.getString("gatewayPaymentId"));

        for (int entrega = 0; entrega < 2; entrega++) {
            given()
                    .contentType(ContentType.JSON)
                    .header("X-Signature", assinar(corpo))
                    .body(corpo)
                    .when()
                    .post("/webhooks/pagamentos/fake")
                    .then()
                    .statusCode(200);
        }

        given().when().get("/pagamentos/" + pagamentoId).then()
                .statusCode(200)
                .body("status", equalTo("PAGO"))
                .body("pagoEm", notNullValue());

        given().when().get("/pedidos/compras/me").then()
                .statusCode(200)
                .body("find { it.id == " + pedidoId + " }.status", equalTo("PAGO"))
                .body("find { it.id == " + pedidoId + " }.pagamentos", hasSize(1));
    }

    @Test
    @TestJwt
    void chaveRepetida_retorna200ComOMesmoPagamento() {
        int pedidoId = criarPedido();
        String chave = UUID.randomUUID().toString();
        String corpo = "{\"metodo\":\"CARTAO_CREDITO\",\"tokenCartao\":\"tok_ok\",\"parcelas\":2}";

        int id = given()
                .contentType(ContentType.JSON).header("Idempotency-Key", chave).body(corpo)
                .when().post("/pedidos/" + pedidoId + "/pagamentos")
                .then().statusCode(201)
                .body("status", equalTo("PAGO"))
                .body("cartao.bandeira", equalTo("VISA"))
                .body("cartao.parcelas", equalTo(2))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON).header("Idempotency-Key", chave).body(corpo)
                .when().post("/pedidos/" + pedidoId + "/pagamentos")
                .then().statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    @TestJwt
    void gatewayIndisponivel_retorna503() {
        int pedidoId = criarPedido();

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"metodo\":\"CARTAO_CREDITO\",\"tokenCartao\":\"tok_erro\"}")
                .when()
                .post("/pedidos/" + pedidoId + "/pagamentos")
                .then()
                .statusCode(503);
    }

    @Test
    @TestJwt
    void pedidoInexistente_retorna404() {
        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"metodo\":\"PIX\"}")
                .when()
                .post("/pedidos/999999/pagamentos")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void pagamentoInexistente_retorna404() {
        given().when().post("/pagamentos/admin/999999/simular-pagamento").then().statusCode(404);
        given().when().post("/pagamentos/admin/999999/estorno").then().statusCode(404);
        given().when().get("/pagamentos/admin/999999/transacoes").then().statusCode(404);
    }

    private int criarPedido() {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "itens":[{"idPanela":%d,"quantidade":2}],
                          "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cep":"77000-000","idCidade":%d}
                        }
                        """.formatted(panelaId, cidadeId))
                .when()
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("status", equalTo("AGUARDANDO_PAGAMENTO"))
                .body("valorTotal", equalTo(160.0f))
                .body("enderecoEntrega.cidade", equalTo("Palmas"))
                .extract().path("id");
    }

    private String assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
