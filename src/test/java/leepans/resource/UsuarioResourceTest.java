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
import leepans.dto.usuario.CadastroSimplesDTO;
import leepans.model.Perfil;
import leepans.model.Usuario;
import leepans.service.ecommerce.UsuarioService;
import leepans.support.TestJwt;

@QuarkusTest
class UsuarioResourceTest {

    private static final String BASE = "/usuarios";

    @InjectMock
    UsuarioService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    void cadastroSimples_deveRetornar201() {
        when(service.create(any(CadastroSimplesDTO.class))).thenReturn(usuario(1L));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Cliente\",\"login\":\"novo@test.com\",\"senha\":\"SenhaForte123\"}")
                .when()
                .post(BASE + "/cadastro/simples")
                .then()
                .statusCode(201)
                .body("id", equalTo(1));
    }

    @Test
    void cadastroCompleto_deveRetornar201() {
        when(service.create(any(leepans.dto.usuario.CadastroCompletoDTO.class))).thenReturn(usuario(2L));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nome":"Cliente",
                          "sobrenome":"Completo",
                          "login":"completo@test.com",
                          "senha":"SenhaForte123",
                          "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cep":"77000-000","idCidade":1}
                        }
                        """)
                .when()
                .post(BASE + "/cadastro/completo")
                .then()
                .statusCode(201)
                .body("id", equalTo(2));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void listarAdmin_deveRetornar200() {
        when(service.findAll()).thenReturn(List.of(usuario(1L)));

        given().accept(ContentType.JSON).when().get(BASE + "/admin").then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].login", equalTo("cliente@test.com"));
    }

    @Test
    @TestSecurity(user = "func", roles = "FUNCIONARIO")
    void buscarPorIdAdmin_deveRetornar200() {
        when(service.findById(1L)).thenReturn(usuario(1L));

        given().accept(ContentType.JSON).when().get(BASE + "/admin/1").then()
                .statusCode(200)
                .body("login", equalTo("cliente@test.com"));
    }

    @Test
    @TestJwt
    void editarEndereco_deveRetornar204() {
        doNothing().when(service).setEndereco(eq(TestJwt.LOGIN), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"logradouro\":\"Rua B\",\"numero\":\"20\",\"bairro\":\"Centro\",\"cep\":\"77000-001\",\"idCidade\":1}")
                .when()
                .patch(BASE + "/editar/enderecos")
                .then()
                .statusCode(204);
    }

    @Test
    @TestJwt
    void editarSenha_deveRetornar204() {
        doNothing().when(service).setPassword(eq(TestJwt.LOGIN), eq("token"), any());

        given()
                .contentType(ContentType.JSON)
                .body("\"NovaSenha123\"")
                .when()
                .patch(BASE + "/editar/senha/token")
                .then()
                .statusCode(204);
    }

    @Test
    void resetSenha_deveRetornar204() {
        doNothing().when(service).resetPassword("token", "NovaSenha123");

        given()
                .contentType(ContentType.JSON)
                .body("\"NovaSenha123\"")
                .when()
                .patch(BASE + "/reset/senha/token")
                .then()
                .statusCode(204);
    }

    @Test
    @TestJwt
    void editarDados_deveRetornar204() {
        doNothing().when(service).setInfo(eq(TestJwt.LOGIN), any());

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Novo\",\"sobrenome\":\"Nome\"}")
                .when()
                .patch(BASE + "/editar/dados")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void remover_deveRetornar204() {
        doNothing().when(service).delete(1L);

        given().when().delete(BASE + "/admin/1").then().statusCode(204);
    }

    @Test
    void listarAdmin_deveRetornar401SemAutenticacao() {
        given().when().get(BASE + "/admin").then().statusCode(401);
    }

    private static Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Cliente");
        u.setLogin(TestJwt.LOGIN);
        u.setPerfil(Perfil.CLIENTE);
        return u;
    }
}
