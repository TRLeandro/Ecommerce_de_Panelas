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
import leepans.model.Cidade;
import leepans.model.Estado;
import leepans.model.Regiao;
import leepans.service.ecommerce.CidadeService;

@QuarkusTest
class CidadeResourceTest {

    private static final String ADMIN = "/cidades/admin";

    @InjectMock
    CidadeService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listar_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(cidade(1L, "Palmas")));

        given().accept(ContentType.JSON).when().get(ADMIN).then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", equalTo("Palmas"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorId_deveRetornar200() {
        when(service.findById(1L)).thenReturn(cidade(1L, "Palmas"));

        given().accept(ContentType.JSON).when().get(ADMIN + "/1").then()
                .statusCode(200)
                .body("nome", equalTo("Palmas"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorNome_deveRetornar200() {
        when(service.findByNome("Palmas")).thenReturn(List.of(cidade(1L, "Palmas")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/nome/Palmas").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorEstado_deveRetornar200() {
        when(service.findByEstadoId(1L)).thenReturn(List.of(cidade(1L, "Palmas")));

        given().accept(ContentType.JSON).when().get(ADMIN + "/estado/1").then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void criar_deveRetornar201() {
        when(service.create(any())).thenReturn(cidade(2L, "Araguaína"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Araguaína\",\"idEstado\":1}")
                .when()
                .post(ADMIN)
                .then()
                .statusCode(201)
                .body("id", equalTo(2));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void atualizar_deveRetornar204() {
        when(service.findById(1L)).thenReturn(cidade(1L, "Palmas"));
        doNothing().when(service).update(any(Long.class), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Palmas Nova\",\"idEstado\":1,\"version\":1}")
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

    private static Cidade cidade(Long id, String nome) {
        Cidade c = new Cidade();
        c.setId(id);
        c.setNome(nome);

        Regiao r = new Regiao();
        r.setId(1L);
        r.setNome("Norte");

        Estado e = new Estado();
        e.setId(1L);
        e.setNome("Tocantins");
        e.setSigla("TO");
        e.setRegiao(r);

        c.setEstado(e);
        return c;
    }
}

