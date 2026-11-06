package leepans.resource;

import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import leepans.exception.AuthorizationException;
import leepans.exception.ProblemDetailSupport;
import leepans.exception.ProblemTypes;
import leepans.exception.ResourceNotFoundException;
import leepans.service.ecommerce.PagamentoService;
import leepans.service.pagamento.gateway.EventoGateway;
import leepans.service.pagamento.gateway.GatewayPagamento;
import leepans.service.pagamento.gateway.GatewaySelecionado;

/**
 * Público porque quem chama é o gateway: a autenticação é a assinatura do corpo.
 * Qualquer resposta diferente de 2xx faz o gateway reenviar o evento.
 */
@Path("/webhooks/pagamentos")
public class WebhookResource {

    @Inject
    @GatewaySelecionado
    GatewayPagamento gateway;

    @Inject
    PagamentoService service;

    @Inject
    ProblemDetailSupport problemDetailSupport;

    @Context
    UriInfo uriInfo;

    @POST
    @Path("/{gateway}")
    @PermitAll
    @Consumes(MediaType.WILDCARD)
    public Response receber(@PathParam("gateway") String nomeGateway, String corpo, @Context HttpHeaders headers) {
        if (!gateway.nome().equals(nomeGateway)) {
            throw new ResourceNotFoundException("Gateway", nomeGateway);
        }

        EventoGateway evento;
        try {
            evento = gateway.interpretarWebhook(corpo, cabecalhos(headers));
        } catch (AuthorizationException e) {
            return problemDetailSupport.toResponse(401, "Assinatura inválida", e.getMessage(),
                    ProblemTypes.UNAUTHORIZED, uriInfo);
        }

        service.processarEventoGateway(evento, nomeGateway);
        return Response.ok().build();
    }

    /** Nomes de header não diferenciam maiúsculas; o mapa também não, para os adaptadores não dependerem disso. */
    private static Map<String, String> cabecalhos(HttpHeaders headers) {
        Map<String, String> mapa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.getRequestHeaders().forEach((nome, valores) -> mapa.put(nome, valores.get(0)));
        return mapa;
    }
}
