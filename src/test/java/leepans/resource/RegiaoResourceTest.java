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
import leepans.model.Regiao;
import leepans.service.ecommerce.RegiaoService;

@QuarkusTest
class RegiaoResourceTest {

    private static final String ADMIN = "/regioes/admin";

    @InjectMock
    RegiaoService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listar_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(regiao(1L, "Norte")));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", equalTo("Norte"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorId_deveRetornar200() {
        when(service.findById(1L)).thenReturn(regiao(1L, "Norte"));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("nome", equalTo("Norte"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorNome_deveRetornar200() {
        when(service.findByNome("Norte")).thenReturn(List.of(regiao(1L, "Norte")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/nome/Norte").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void criar_deveRetornar201() {
        when(service.create(any())).thenReturn(regiao(2L, "Nordeste"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Nordeste\"}")
                .when()
                .post(ADMIN)
                .then()
                .statusCode(201)
                .body("id", equalTo(2));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizar_deveRetornar204() {
        when(service.findById(1L)).thenReturn(regiao(1L, "Norte"));
        doNothing().when(service).update(any(Long.class), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Sul\",\"version\":1}")
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

    private static Regiao regiao(Long id, String nome) {
        Regiao r = new Regiao();
        r.setId(id);
        r.setNome(nome);
        return r;
    }
}

