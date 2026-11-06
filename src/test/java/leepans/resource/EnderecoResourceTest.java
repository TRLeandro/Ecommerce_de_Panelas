package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import leepans.model.Cidade;
import leepans.model.Endereco;
import leepans.model.Estado;
import leepans.model.Regiao;
import leepans.service.ecommerce.EnderecoService;
import leepans.support.TestJwt;

@QuarkusTest
class EnderecoResourceTest {

    private static final String ADMIN = "/enderecos/admin";

    @InjectMock
    EnderecoService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listar_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(endereco(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].cidade.nome", equalTo("Palmas"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorId_deveRetornar200() {
        when(service.findById(1L)).thenReturn(endereco(1L));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("cep", equalTo("77000-000"));
    }

    @Test
    @TestJwt
    void buscarPorUsuario_deveRetornar200() {
        when(service.findByUsuario(TestJwt.LOGIN)).thenReturn(endereco(1L));

        given().accept(ContentType.JSON).when().get("/enderecos/usuarios/me").then()
                .statusCode(200)
                .body("cidade.nome", equalTo("Palmas"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorCidade_deveRetornar200() {
        when(service.findByCidade("Palmas")).thenReturn(List.of(endereco(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/cidade/Palmas").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorEstado_deveRetornar200() {
        when(service.findByEstado("TO")).thenReturn(List.of(endereco(1L)));

        given().accept(ContentType.JSON).when().get(ADMIN + "/estado/TO").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void criar_deveRetornar201() {
        when(service.create(any())).thenReturn(endereco(2L));

        given()
                .contentType(ContentType.JSON)
                .body(enderecoPayload())
                .when()
                .post(ADMIN)
                .then()
                .statusCode(201)
                .body("id", equalTo(2));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizar_deveRetornar204() {
        when(service.findById(1L)).thenReturn(endereco(1L));
        doNothing().when(service).update(eq(1L), any());

        given()
                .contentType(ContentType.JSON)
                .body(enderecoPayload().replace("}", ",\"version\":1}"))
                .when()
                .put(ADMIN + "/1")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void remover_deveRetornar204() {
        doNothing().when(service).delete(1L);

        given().when().delete(ADMIN + "/1").then().statusCode(204);
    }

    private static String enderecoPayload() {
        return """
                {
                  "logradouro":"Rua A",
                  "numero":"100",
                  "complemento":"Apto 1",
                  "bairro":"Centro",
                  "cep":"77000-000",
                  "idCidade":1
                }
                """;
    }

    private static Endereco endereco(Long id) {
        Endereco e = new Endereco();
        e.setId(id);
        e.setLogradouro("Rua A");
        e.setNumero("100");
        e.setComplemento("Apto 1");
        e.setBairro("Centro");
        e.setCep("77000-000");

        Regiao r = new Regiao();
        r.setId(1L);
        r.setNome("Norte");

        Estado est = new Estado();
        est.setId(1L);
        est.setNome("Tocantins");
        est.setSigla("TO");
        est.setRegiao(r);

        Cidade c = new Cidade();
        c.setId(1L);
        c.setNome("Palmas");
        c.setEstado(est);

        e.setCidade(c);
        return e;
    }
}
