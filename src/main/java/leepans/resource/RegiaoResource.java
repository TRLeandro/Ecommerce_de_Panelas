package leepans.resource;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import leepans.dto.regiao.RegiaoRequestDTO;
import leepans.dto.regiao.RegiaoResponseDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.mapper.RegiaoMapper;
import leepans.model.Regiao;
import leepans.service.ecommerce.RegiaoService;

@Path("/regioes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegiaoResource {

    @Inject
    RegiaoService service;

    @POST
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response create(@Valid RegiaoRequestDTO dto) {
        Regiao regiao = service.create(RegiaoMapper.toEntity(dto));
        return Response.status(Status.CREATED).entity(RegiaoMapper.toResponseDTO(regiao)).build();
    }

    @GET
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findAll() {
        List<RegiaoResponseDTO> lista = service.findAll()
                .stream()
                .map(RegiaoMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findById(@PathParam("id") Long id) {
        Regiao regiao = service.findById(id);
        if (regiao == null) {
            throw new ResourceNotFoundException("Região", id);
        }
        return Response.ok(RegiaoMapper.toResponseDTO(regiao)).build();
    }

    @GET
    @Path("/admin/nome/{nome}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByNome(@PathParam("nome") String nome) {
        List<RegiaoResponseDTO> lista = service.findByNome(nome)
                .stream()
                .map(RegiaoMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @PUT
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response update(@PathParam("id") Long id, @Valid RegiaoRequestDTO dto) {
        if (dto.version() == null) {
            throw new ValidationException("A versão da região é obrigatória para atualização.", "version");
        }
        service.update(id, dto);
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

