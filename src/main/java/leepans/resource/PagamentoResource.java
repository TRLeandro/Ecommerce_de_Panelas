package leepans.resource;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import leepans.dto.pagamento.PagamentoEcommerceDTO;
import leepans.dto.pagamento.PagamentoResponseDTO;
import leepans.dto.pagamento.TransacaoGatewayResponseDTO;
import leepans.mapper.PagamentoMapper;
import leepans.model.StatusPagamento;
import leepans.service.ecommerce.PagamentoService;

@Path("/pagamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PagamentoResource {

    @Inject
    PagamentoService service;

    @Inject
    PagamentoMapper pagamentoMapper;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{id}")
    @RolesAllowed({ "CLIENTE" })
    public Response findMeuPagamento(@PathParam("id") Long id) {
        String login = jwt.getClaim("upn");
        return Response.ok(pagamentoMapper.toResponse(service.findByIdDoUsuario(id, login))).build();
    }

    @GET
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findAll() {
        List<PagamentoResponseDTO> lista = service.findAll()
                .stream()
                .map(pagamentoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findById(@PathParam("id") Long id) {
        PagamentoResponseDTO pagamento = pagamentoMapper.toResponse(service.findById(id));
        return Response.ok(pagamento).build();
    }

    @GET
    @Path("/admin/status/{status}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByStatus(@PathParam("status") StatusPagamento statusPagamento) {
        List<PagamentoResponseDTO> lista = service.findByStatusPagamento(statusPagamento)
                .stream()
                .map(pagamentoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}/transacoes")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findTransacoes(@PathParam("id") Long id) {
        List<TransacaoGatewayResponseDTO> lista = service.findTransacoes(id)
                .stream()
                .map(pagamentoMapper::toResponse)
                .toList();
        return Response.ok(lista).build();
    }

    @POST
    @Path("/admin/{id}/estorno")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN" })
    public Response estornar(@PathParam("id") Long id) {
        service.estornar(id);
        return Response.ok(pagamentoMapper.toResponse(service.findById(id))).build();
    }

    @POST
    @Path("/admin/{id}/simular-pagamento")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response simularPagamento(@PathParam("id") Long id) {
        service.simularPagamento(id);
        return Response.ok(pagamentoMapper.toResponse(service.findById(id))).build();
    }

    @GET
    @Path("/ecommerce")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO", "CLIENTE" })
    public Response findPagamentosEcommerce() {
        String login = jwt.getClaim("upn");
        List<PagamentoEcommerceDTO> lista = service.findByUsuario(login)
                .stream()
                .map(pagamentoMapper::toEcommerceDTO)
                .toList();
        return Response.ok(lista).build();
    }
}
