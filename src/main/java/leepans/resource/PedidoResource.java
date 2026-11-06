package leepans.resource;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import leepans.dto.pagamento.IniciarPagamentoDTO;
import leepans.dto.pedido.PedidoRequestDTO;
import leepans.dto.pedido.PedidoResponseDTO;
import leepans.exception.ValidationException;
import leepans.mapper.EnderecoMapper;
import leepans.mapper.PagamentoMapper;
import leepans.mapper.PedidoMapper;
import leepans.model.Endereco;
import leepans.model.Pagamento;
import leepans.model.Pedido;
import leepans.model.StatusPedido;
import leepans.service.ecommerce.PagamentoService;
import leepans.service.ecommerce.PedidoService;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    private static final int TAMANHO_MAXIMO_CHAVE = 255;

    @Inject
    PedidoService service;

    @Inject
    PagamentoService pagamentoService;

    @Inject
    PedidoMapper pedidoMapper;

    @Inject
    PagamentoMapper pagamentoMapper;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findAll() {
        List<PedidoResponseDTO> lista = service.findAll()
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findById(@PathParam("id") Long id) {
        PedidoResponseDTO pedido = pedidoMapper.toResponse(service.findById(id));
        return Response.ok(pedido).build();
    }

    @GET
    @Path("/compras/me")
    @Authenticated
    public Response findCompras() {
        String login = jwt.getClaim("upn");
        List<PedidoResponseDTO> lista = service.findCompras(login)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/compras/me/status/{status}")
    @Authenticated
    public Response findComprasPorStatus(@PathParam("status") StatusPedido status) {
        String login = jwt.getClaim("upn");
        List<PedidoResponseDTO> lista = service.findCompras(login, status)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/usuarios/{usuarioId}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByUsuarioId(@PathParam("usuarioId") Long usuarioId) {
        List<PedidoResponseDTO> lista = service.findByUsuarioId(usuarioId)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/status/{status}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByStatus(@PathParam("status") StatusPedido status) {
        List<PedidoResponseDTO> lista = service.findByStatus(status)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/enderecos/cidades")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByEnderecoCidade(@QueryParam("cidade") String cidade) {
        if (cidade == null || cidade.isBlank()) {
            throw new ValidationException("Cidade é obrigatória para filtrar por endereço.", "cidade");
        }
        List<PedidoResponseDTO> lista = service.findByEnderecoCidade(cidade)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @POST
    @Authenticated
    public Response create(@Valid PedidoRequestDTO dto) {
        String login = jwt.getClaim("upn");
        Endereco endereco = EnderecoMapper.toEntity(dto.endereco());
        Pedido pedido = service.create(pedidoMapper.toEntity(dto), login, endereco, dto.codigoCupomDesconto());
        return Response.status(201)
                .entity(pedidoMapper.toResponse(pedido))
                .build();
    }

    @POST
    @Path("/{id}/pagamentos")
    @RolesAllowed({ "CLIENTE" })
    public Response iniciarPagamento(
            @PathParam("id") Long id,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @NotNull(message = "O corpo da requisição é obrigatório") @Valid IniciarPagamentoDTO dto) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > TAMANHO_MAXIMO_CHAVE) {
            throw new ValidationException(
                    "O header Idempotency-Key é obrigatório e deve ter até " + TAMANHO_MAXIMO_CHAVE + " caracteres.",
                    "Idempotency-Key");
        }

        String login = jwt.getClaim("upn");
        boolean repetido = pagamentoService.existeIdempotencyKey(idempotencyKey);
        Pagamento pagamento = pagamentoService.iniciar(id, login, dto, idempotencyKey);
        return Response.status(repetido ? 200 : 201)
                .entity(pagamentoMapper.toResponse(pagamento))
                .build();
    }

    @POST
    @Path("/admin/{id}/separacao")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response iniciarSeparacao(@PathParam("id") Long id) {
        service.iniciarSeparacao(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/admin/{id}/envio")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response marcarComoEnviado(@PathParam("id") Long id) {
        service.marcarComoEnviado(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/admin/{id}/entrega")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response marcarComoEntregue(@PathParam("id") Long id) {
        service.marcarComoEntregue(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/admin/{id}/cancelamento")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response cancelar(@PathParam("id") Long id) {
        service.cancelar(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN" })
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
