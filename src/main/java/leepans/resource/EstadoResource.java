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
import leepans.dto.estado.EstadoRequestDTO;
import leepans.dto.estado.EstadoResponseDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.exception.ValidationException;
import leepans.mapper.EstadoMapper;
import leepans.model.Estado;
import leepans.service.ecommerce.EstadoService;

@Path("/estados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EstadoResource {

    @Inject
    EstadoService service;

    @POST
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response create(@Valid EstadoRequestDTO dto) {
        Estado estado = service.create(EstadoMapper.toEntity(dto));
        return Response.status(Status.CREATED).entity(EstadoMapper.toResponseDTO(estado)).build();
    }

    @GET
    @Path("/admin")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findAll() {
        List<EstadoResponseDTO> lista = service.findAll()
                .stream()
                .map(EstadoMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findById(@PathParam("id") Long id) {
        Estado estado = service.findById(id);
        if (estado == null) {
            throw new ResourceNotFoundException("Estado", id);
        }
        return Response.ok(EstadoMapper.toResponseDTO(estado)).build();
    }

    @GET
    @Path("/admin/nome/{nome}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByNome(@PathParam("nome") String nome) {
        List<EstadoResponseDTO> lista = service.findByNome(nome)
                .stream()
                .map(EstadoMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/admin/sigla/{sigla}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findBySigla(@PathParam("sigla") String sigla) {
        Estado estado = service.findBySigla(sigla);
        return Response.ok(EstadoMapper.toResponseDTO(estado)).build();
    }

    @GET
    @Path("/admin/regiao/{idRegiao}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response findByRegiaoId(@PathParam("idRegiao") Long idRegiao) {
        List<EstadoResponseDTO> lista = service.findByRegiaoId(idRegiao)
                .stream()
                .map(EstadoMapper::toResponseDTO)
                .toList();
        return Response.ok(lista).build();
    }

    @PUT
    @Path("/admin/{id}")
    @RolesAllowed({ "ADMIN", "FUNCIONARIO" })
    public Response update(@PathParam("id") Long id, @Valid EstadoRequestDTO dto) {
        if (dto.version() == null) {
            throw new ValidationException("A versão do estado é obrigatória para atualização.", "version");
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

