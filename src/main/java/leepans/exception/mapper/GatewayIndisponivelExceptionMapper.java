package leepans.exception.mapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import leepans.exception.ProblemDetailSupport;
import leepans.exception.ProblemTypes;
import leepans.service.pagamento.gateway.GatewayIndisponivelException;

@Provider
public class GatewayIndisponivelExceptionMapper implements ExceptionMapper<GatewayIndisponivelException> {

    @Inject
    ProblemDetailSupport problemDetailSupport;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(GatewayIndisponivelException exception) {
        return problemDetailSupport.toResponse(
                503,
                "Gateway de pagamento indisponível",
                "Não foi possível falar com o gateway de pagamento. Tente novamente em instantes.",
                ProblemTypes.GATEWAY_UNAVAILABLE,
                uriInfo
        );
    }
}
