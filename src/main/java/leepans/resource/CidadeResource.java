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
import leepans.dto.cidade.CidadeRequestDTO;
import leepans.dto.cidade.CidadeResponseDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.mapper.CidadeMapper;
import leepans.model.Cidade;
import leepans.service.ecommerce.CidadeService;

@Path("/cidades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CidadeResource {

    @Inject
    CidadeService service;

    @POST
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response create(@Valid CidadeRequestDTO dto) {
        Cidade cidade = service.create(CidadeMapper.toEntity(dto));
        return Response.status(Status.CREATED).entity(CidadeMapper.toResponseDTO(cidade)).build();
    }

    @GET
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findAll() {
        List<CidadeResponseDTO> lista = service.findAll()
                .stream()
                .map(CidadeMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findById(@PathParam("id") Long id) {
        Cidade cidade = service.findById(id);
        if (cidade == null) {
            throw new ResourceNotFoundException("Cidade", id);
        }
        return Response.ok(CidadeMapper.toResponseDTO(cidade)).build();
    }

    @GET
    @Path("/admin/nome/{nome}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByNome(@PathParam("nome") String nome) {
        List<CidadeResponseDTO> lista = service.findByNome(nome)
                .stream()
                .map(CidadeMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/estado/{idEstado}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByEstadoId(@PathParam("idEstado") Long idEstado) {
        List<CidadeResponseDTO> lista = service.findByEstadoId(idEstado)
                .stream()
                .map(CidadeMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @PUT
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response update(@PathParam("id") Long id, @Valid CidadeRequestDTO dto) {
        if (dto.version() == null) {
            throw new ValidationException("A versão da cidade é obrigatória para atualização.", "version");
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

