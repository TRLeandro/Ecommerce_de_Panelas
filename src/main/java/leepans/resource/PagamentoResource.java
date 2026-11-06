package leepans.resource;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import leepans.dto.pagamento.BoletoRequestDTO;
import leepans.dto.pagamento.CartaoRequestDTO;
import leepans.dto.pagamento.PagamentoEcommerceDTO;
import leepans.dto.pagamento.PagamentoPatchDTO;
import leepans.dto.pagamento.PagamentoResponseDTO;
import leepans.dto.pagamento.PixRequestDTO;
import leepans.exception.ValidationException;
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

    @Context
    SecurityContext securityContext;

    /** ADMIN e FUNCIONARIO acessam qualquer ID; os demais só os próprios pagamentos. */
    private void verificarAcesso(Long id) {
        if (!securityContext.isUserInRole("ADMIN") && !securityContext.isUserInRole("FUNCIONARIO")) {
            service.findByIdDoUsuario(id, jwt.getClaim("upn"));
        }
    }

    @PUT
    @Path("/admin/processar/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response processarPagamento(@PathParam("id") Long id) {
        service.processarPagamento(service.findById(id));
        return Response.noContent().build();
    }
    
    @PUT
    @Path("/cartao/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO", "CLIENTE" })
    public Response completeInfoCartao(@PathParam("id") Long id, @Valid CartaoRequestDTO dto) {
        verificarAcesso(id);
        service.completeInfo(id, dto);
        return Response.noContent().build();
    }

    @PUT
    @Path("/boleto/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO", "CLIENTE" })
    public Response completeInfoBoleto(@PathParam("id") Long id, @Valid BoletoRequestDTO dto) {
        verificarAcesso(id);
        service.completeInfo(id, dto);
        return Response.noContent().build();
    }

    @PUT
    @Path("/pix/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO", "CLIENTE" })
    public Response completeInfoPix(@PathParam("id") Long id, @Valid PixRequestDTO dto) {
        verificarAcesso(id);
        service.completeInfo(id, dto);
        return Response.noContent().build();
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
    @Path("/ecommerce")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO", "CLIENTE" })
    public Response findPagamentosEcommerce(@PathParam("id") Long id) {
        String login = jwt.getClaim("upn");
        List<PagamentoEcommerceDTO> lista = service.findByUsuario(login)
                .stream()
                .map(pagamentoMapper::toEcommerceDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @PATCH
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response setStatus(@PathParam("id") Long id, @Valid PagamentoPatchDTO dto) {
        if (dto.version() == null) {
            throw new ValidationException(
                    "A versão do pagamento é obrigatória para atualização.",
                    "version"
            );
        }
        service.setStatus(id, dto);
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
