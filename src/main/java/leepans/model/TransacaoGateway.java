package leepans.model;

import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Log auditável: uma linha por interação com o gateway.
 */
@Entity
public class TransacaoGateway extends DefaultEntity {

    private static final String MASCARA = "***";
    private static final Pattern CAMPO_TOKEN = Pattern.compile("(?i)(\"[\\w-]*token[\\w-]*\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern VALOR_TOKEN = Pattern.compile("\\btok_[\\w-]+");

    @ManyToOne(optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private Pagamento pagamento;

    @Column(name = "codigo_tipo_transacao", nullable = false)
    private TipoTransacao tipo;

    @Column(name = "status_gateway")
    private String statusGateway;

    @Column(name = "codigo_retorno")
    private String codigoRetorno;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "payload_bruto", columnDefinition = "TEXT")
    private String payloadBruto;

    protected TransacaoGateway() {
    }

    public TransacaoGateway(Pagamento pagamento, TipoTransacao tipo, String statusGateway, String codigoRetorno,
            String mensagem, String payloadBruto) {
        this.pagamento = pagamento;
        this.tipo = tipo;
        this.statusGateway = statusGateway;
        this.codigoRetorno = codigoRetorno;
        this.mensagem = mascarar(mensagem);
        this.payloadBruto = mascarar(payloadBruto);
    }

    /** Remove tokens de cartão de um texto vindo do gateway antes de ele ser persistido. */
    public static String mascarar(String texto) {
        if (texto == null) {
            return null;
        }
        String semCampos = CAMPO_TOKEN.matcher(texto).replaceAll("$1" + MASCARA + "$2");
        return VALOR_TOKEN.matcher(semCampos).replaceAll(MASCARA);
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public String getStatusGateway() {
        return statusGateway;
    }

    public String getCodigoRetorno() {
        return codigoRetorno;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getPayloadBruto() {
        return payloadBruto;
    }
}
