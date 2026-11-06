package leepans.exception;

/**
 * URIs de tipo ({@code type}) para respostas RFC 7807.
 * O prefixo base pode ser sobrescrito via {@code app.api.problem-base-uri}.
 */
public final class ProblemTypes {

    public static final String DEFAULT_BASE_URI = "urn:leepans:errors";

    public static final String VALIDATION = "validation-error";
    public static final String AUTHORIZATION = "authorization-error";
    public static final String NOT_FOUND = "resource-not-found";
    public static final String CONCURRENCY = "concurrency-conflict";
    public static final String BAD_REQUEST = "bad-request";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String INTERNAL = "internal-server-error";
    public static final String BUSINESS_RULE = "business-rule-violation";
    public static final String GATEWAY_UNAVAILABLE = "payment-gateway-unavailable";

    private ProblemTypes() {
    }
}
