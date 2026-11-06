package leepans.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import leepans.model.Estado;
import leepans.model.Regiao;
import leepans.service.ecommerce.EstadoService;

@QuarkusTest
class EstadoResourceTest {

    private static final String ADMIN = "/estados/admin";

    @InjectMock
    EstadoService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listar_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(estado(1L, "Tocantins", "TO")));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].sigla", equalTo("TO"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorId_deveRetornar200() {
        when(service.findById(1L)).thenReturn(estado(1L, "Tocantins", "TO"));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("sigla", equalTo("TO"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorNome_deveRetornar200() {
        when(service.findByNome("Tocantins")).thenReturn(List.of(estado(1L, "Tocantins", "TO")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/nome/Tocantins").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorSigla_deveRetornar200() {
        when(service.findBySigla("TO")).thenReturn(estado(1L, "Tocantins", "TO"));

        given().accept(ContentType.JSON).when().get(ADMIN + "/sigla/TO").then()
                .statusCode(200)
                .body("nome", equalTo("Tocantins"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorRegiao_deveRetornar200() {
        when(service.findByRegiaoId(1L)).thenReturn(List.of(estado(1L, "Tocantins", "TO")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/regiao/1").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void criar_deveRetornar201() {
        when(service.create(any())).thenReturn(estado(2L, "Goiás", "GO"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Goiás\",\"sigla\":\"GO\",\"idRegiao\":1}")
                .when()
                .post(ADMIN)
                .then()
                .statusCode(201)
                .body("id", equalTo(2));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizar_deveRetornar204() {
        when(service.findById(1L)).thenReturn(estado(1L, "Tocantins", "TO"));
        doNothing().when(service).update(any(Long.class), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Tocantins Novo\",\"sigla\":\"TO\",\"idRegiao\":1,\"version\":1}")
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

    private static Estado estado(Long id, String nome, String sigla) {
        Estado e = new Estado();
        e.setId(id);
        e.setNome(nome);
        e.setSigla(sigla);

        Regiao r = new Regiao();
        r.setId(1L);
        r.setNome("Norte");
        e.setRegiao(r);

        return e;
    }
}

