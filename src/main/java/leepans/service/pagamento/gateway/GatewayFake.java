package leepans.service.pagamento.gateway;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import leepans.exception.AuthorizationException;
import leepans.exception.BadRequestException;
import leepans.model.DadosBoleto;
import leepans.model.DadosCartao;
import leepans.model.DadosPix;
import leepans.model.MetodoPagamento;

/**
 * Simula um gateway de forma determinística, para testes e demonstração. As
 * cobranças ficam em memória: depois de reiniciar a aplicação, uma cobrança
 * desconhecida é tratada como ainda aguardando pagamento.
 */
@ApplicationScoped
public class GatewayFake implements GatewayPagamento {

    public static final String NOME = "fake";

    private static final String HEADER_ASSINATURA = "X-Signature";
    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final int TAMANHO_LINHA_DIGITAVEL = 47;
    private static final int TAMANHO_CODIGO_BARRAS = 44;

    private final Map<String, StatusGateway> cobrancas = new ConcurrentHashMap<>();

    private final String webhookSecret;
    private final int pixExpiracaoMinutos;
    private final int boletoDiasVencimento;
    private final ObjectMapper objectMapper;

    @Inject
    public GatewayFake(
            @ConfigProperty(name = "app.pagamento.webhook-secret") String webhookSecret,
            @ConfigProperty(name = "app.pagamento.pix.expiracao-minutos", defaultValue = "30") int pixExpiracaoMinutos,
            @ConfigProperty(name = "app.pagamento.boleto.dias-vencimento", defaultValue = "3") int boletoDiasVencimento,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.pixExpiracaoMinutos = pixExpiracaoMinutos;
        this.boletoDiasVencimento = boletoDiasVencimento;
        this.objectMapper = objectMapper;
    }

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public ResultadoCobranca criarCobranca(SolicitacaoCobranca solicitacao) {
        ResultadoCobranca resultado;
        if (solicitacao.metodo().isCartao()) {
            resultado = cobrarCartao(solicitacao);
        } else if (solicitacao.metodo() == MetodoPagamento.PIX) {
            resultado = gerarPix(solicitacao);
        } else {
            resultado = gerarBoleto(solicitacao);
        }
        cobrancas.put(resultado.gatewayPaymentId(), resultado.status());
        return resultado;
    }

    private ResultadoCobranca cobrarCartao(SolicitacaoCobranca solicitacao) {
        String token = solicitacao.tokenCartao();
        if (token.startsWith("tok_erro")) {
            throw new GatewayIndisponivelException("Gateway fake indisponível (simulação).");
        }

        String id = novoId();
        if (token.startsWith("tok_recusado")) {
            String codigo = "cc_rejected_insufficient_amount";
            return new ResultadoCobranca(id, StatusGateway.RECUSADO, codigo, "Saldo insuficiente.",
                    null, null, null, null, payload(id, StatusGateway.RECUSADO, solicitacao.valor()));
        }

        String codigoAutorizacao = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        DadosCartao cartao = new DadosCartao("VISA", "4242", solicitacao.parcelas(), codigoAutorizacao);
        return new ResultadoCobranca(id, StatusGateway.PAGO, "approved", "Pagamento aprovado.",
                cartao, null, null, null, payload(id, StatusGateway.PAGO, solicitacao.valor()));
    }

    private ResultadoCobranca gerarPix(SolicitacaoCobranca solicitacao) {
        String id = novoId();
        String txid = UUID.randomUUID().toString();
        String copiaECola = "00020126...FAKE" + txid;
        String qrCode = Base64.getEncoder().encodeToString(copiaECola.getBytes(StandardCharsets.UTF_8));
        return new ResultadoCobranca(id, StatusGateway.AGUARDANDO_PAGAMENTO, "pending", "Aguardando pagamento do Pix.",
                null, new DadosPix(copiaECola, qrCode, txid), null,
                LocalDateTime.now().plusMinutes(pixExpiracaoMinutos),
                payload(id, StatusGateway.AGUARDANDO_PAGAMENTO, solicitacao.valor()));
    }

    private ResultadoCobranca gerarBoleto(SolicitacaoCobranca solicitacao) {
        String id = novoId();
        LocalDate vencimento = LocalDate.now().plusDays(boletoDiasVencimento);
        DadosBoleto boleto = new DadosBoleto(digitosAleatorios(TAMANHO_LINHA_DIGITAVEL),
                digitosAleatorios(TAMANHO_CODIGO_BARRAS), "https://gateway.fake/boletos/" + id + ".pdf", vencimento);
        return new ResultadoCobranca(id, StatusGateway.AGUARDANDO_PAGAMENTO, "pending", "Aguardando pagamento do boleto.",
                null, null, boleto, vencimento.atTime(LocalTime.MAX),
                payload(id, StatusGateway.AGUARDANDO_PAGAMENTO, solicitacao.valor()));
    }

    @Override
    public ResultadoCobranca consultar(String gatewayPaymentId) {
        StatusGateway status = cobrancas.getOrDefault(gatewayPaymentId, StatusGateway.AGUARDANDO_PAGAMENTO);
        return new ResultadoCobranca(gatewayPaymentId, status, null, null, null, null, null, null,
                "{\"id\":\"%s\",\"status\":\"%s\"}".formatted(gatewayPaymentId, status));
    }

    @Override
    public ResultadoEstorno estornar(String gatewayPaymentId, BigDecimal valor) {
        cobrancas.put(gatewayPaymentId, StatusGateway.ESTORNADO);
        return new ResultadoEstorno(true, "Estorno realizado.", payload(gatewayPaymentId, StatusGateway.ESTORNADO, valor));
    }

    @Override
    public void cancelar(String gatewayPaymentId) {
        cobrancas.put(gatewayPaymentId, StatusGateway.CANCELADO);
    }

    /** Simula o cliente pagando a cobrança e devolve o evento que o gateway enviaria. */
    public EventoGateway simularPagamento(String gatewayPaymentId) {
        cobrancas.put(gatewayPaymentId, StatusGateway.PAGO);
        String eventoId = "evt_" + UUID.randomUUID();
        String corpo = "{\"id\":\"%s\",\"paymentId\":\"%s\",\"status\":\"PAGO\"}".formatted(eventoId, gatewayPaymentId);
        return new EventoGateway(eventoId, gatewayPaymentId, StatusGateway.PAGO, corpo);
    }

    /**
     * Um webhook com assinatura válida também atualiza a cobrança em memória:
     * aqui ele representa o próprio gateway mudando de estado, e é isso que a
     * reconsulta feita depois pelo PagamentoService vai enxergar.
     */
    @Override
    public EventoGateway interpretarWebhook(String corpo, Map<String, String> headers) {
        validarAssinatura(corpo, headers.get(HEADER_ASSINATURA));

        JsonNode json = lerJson(corpo);
        String eventoId = json.path("id").asText(null);
        String gatewayPaymentId = json.path("paymentId").asText(null);
        StatusGateway status = lerStatus(json.path("status").asText(""));
        if (eventoId == null || gatewayPaymentId == null) {
            throw new BadRequestException("Corpo do webhook sem id ou paymentId.");
        }

        cobrancas.put(gatewayPaymentId, status);
        return new EventoGateway(eventoId, gatewayPaymentId, status, corpo);
    }

    private void validarAssinatura(String corpo, String assinatura) {
        if (assinatura == null) {
            throw new AuthorizationException("Assinatura do webhook ausente.");
        }
        byte[] esperada = assinar(corpo).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, assinatura.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthorizationException("Assinatura do webhook inválida.");
        }
    }

    /** HMAC-SHA256 em hexadecimal, o mesmo formato de {@code openssl dgst -sha256 -hmac}. */
    String assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC));
            return HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o HMAC do webhook.", e);
        }
    }

    private JsonNode lerJson(String corpo) {
        try {
            return objectMapper.readTree(corpo);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Corpo do webhook não é um JSON válido.");
        }
    }

    private StatusGateway lerStatus(String status) {
        try {
            return StatusGateway.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status do webhook desconhecido: " + status);
        }
    }

    private static String novoId() {
        return "fake_" + UUID.randomUUID();
    }

    private static String digitosAleatorios(int quantidade) {
        return ThreadLocalRandom.current().ints(quantidade, 0, 10)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /** A conversão para centavos existe só aqui: no domínio o dinheiro é BigDecimal. */
    private static String payload(String id, StatusGateway status, BigDecimal valor) {
        return "{\"id\":\"%s\",\"status\":\"%s\",\"amount\":%d}"
                .formatted(id, status, valor.movePointRight(2).longValueExact());
    }
}
