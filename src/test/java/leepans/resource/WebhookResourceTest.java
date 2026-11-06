package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import leepans.service.ecommerce.PagamentoService;
import leepans.service.pagamento.gateway.EventoGateway;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;
import leepans.service.pagamento.gateway.StatusGateway;

@QuarkusTest
class WebhookResourceTest {

    private static final String URL = "/webhooks/pagamentos/fake";
    private static final String CORPO = "{ \"id\":\"evt_1\",  \"paymentId\":\"fake_1\", \"status\":\"PAGO\" }";

    @InjectMock
    PagamentoService service;

    @ConfigProperty(name = "app.pagamento.webhook-secret")
    String segredo;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    void assinaturaValida_deveRetornar200EProcessar() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", assinar(CORPO))
                .body(CORPO)
                .when()
                .post(URL)
                .then()
                .statusCode(200);

        ArgumentCaptor<EventoGateway> evento = ArgumentCaptor.forClass(EventoGateway.class);
        verify(service).processarEventoGateway(evento.capture(), eq("fake"));
        org.junit.jupiter.api.Assertions.assertEquals("evt_1", evento.getValue().eventoId());
        org.junit.jupiter.api.Assertions.assertEquals(StatusGateway.PAGO, evento.getValue().novoStatus());
    }

    @Test
    void headerEmMinusculas_deveSerAceito() {
        given()
                .contentType(ContentType.JSON)
                .header("x-signature", assinar(CORPO))
                .body(CORPO)
                .when()
                .post(URL)
                .then()
                .statusCode(200);
    }

    @Test
    void assinaturaInvalida_deveRetornar401() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", "invalida")
                .body(CORPO)
                .when()
                .post(URL)
                .then()
                .statusCode(401);

        verifyNoInteractions(service);
    }

    @Test
    void corpoAlterado_deveRetornar401() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", assinar(CORPO))
                .body(CORPO.replace("PAGO", "ESTORNADO"))
                .when()
                .post(URL)
                .then()
                .statusCode(401);
    }

    @Test
    void semAssinatura_deveRetornar401() {
        given().contentType(ContentType.JSON).body(CORPO).when().post(URL).then().statusCode(401);
    }

    @Test
    void gatewayDiferenteDoConfigurado_deveRetornar404() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", assinar(CORPO))
                .body(CORPO)
                .when()
                .post("/webhooks/pagamentos/mercadopago")
                .then()
                .statusCode(404);

        verifyNoInteractions(service);
    }

    @Test
    void corpoMalformadoAssinado_deveRetornar400() {
        String corpo = "isto nao e json";

        given()
                .contentType(ContentType.TEXT)
                .header("X-Signature", assinar(corpo))
                .body(corpo)
                .when()
                .post(URL)
                .then()
                .statusCode(400);
    }

    @Test
    void erroInesperado_deveRetornar500() {
        doThrow(new IllegalMonitorStateException("bug")).when(service).processarEventoGateway(any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", assinar(CORPO))
                .body(CORPO)
                .when()
                .post(URL)
                .then()
                .statusCode(500);
    }

    @Test
    void gatewayForaDoArNaReconsulta_naoRetorna2xx() {
        doThrow(new GatewayIndisponivelException("fora do ar")).when(service).processarEventoGateway(any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("X-Signature", assinar(CORPO))
                .body(CORPO)
                .when()
                .post(URL)
                .then()
                .statusCode(503);
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
