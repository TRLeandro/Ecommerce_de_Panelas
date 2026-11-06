package leepans.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import leepans.exception.BusinessRuleViolationException;

/**
 * Uma tentativa de pagamento de um pedido. O status só muda pelos métodos de
 * transição desta classe; cada um rejeita a mudança se ela não for válida.
 */
@Entity
@Table(indexes = @Index(name = "idx_pagamento_gateway_payment_id", columnList = "gateway_payment_id"))
public class Pagamento extends DefaultEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "codigo_metodo_pagamento", nullable = false)
    private MetodoPagamento metodo;

    @Column(name = "codigo_status_pagamento", nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "gateway", nullable = false)
    private String gateway;

    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "motivo_recusa", columnDefinition = "TEXT")
    private String motivoRecusa;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Embedded
    private DadosCartao cartao;

    @Embedded
    private DadosPix pix;

    @Embedded
    private DadosBoleto boleto;

    @OneToMany(mappedBy = "pagamento", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<TransacaoGateway> transacoes = new ArrayList<>();

    protected Pagamento() {
    }

    public Pagamento(Pedido pedido, MetodoPagamento metodo, BigDecimal valor, String gateway, String idempotencyKey) {
        this.pedido = pedido;
        this.metodo = metodo;
        this.valor = valor;
        this.gateway = gateway;
        this.idempotencyKey = idempotencyKey;
    }

    public void aguardarPagamento(LocalDateTime expiraEm) {
        exigirStatus("aguardar pagamento", StatusPagamento.PENDENTE);
        this.status = StatusPagamento.AGUARDANDO_PAGAMENTO;
        this.expiraEm = expiraEm;
    }

    public void autorizar(String codigoAutorizacao) {
        exigirStatus("autorizar", StatusPagamento.PENDENTE);
        this.status = StatusPagamento.AUTORIZADO;
        if (this.cartao == null) {
            this.cartao = new DadosCartao();
        }
        this.cartao.setCodigoAutorizacao(codigoAutorizacao);
    }

    /** Confirmar o que já está pago não faz nada: webhooks se repetem. */
    public void confirmar(LocalDateTime quando) {
        if (this.status == StatusPagamento.PAGO) {
            return;
        }
        exigirStatus("confirmar", StatusPagamento.AGUARDANDO_PAGAMENTO, StatusPagamento.AUTORIZADO,
                StatusPagamento.PENDENTE);
        this.pedido.marcarComoPago();
        this.status = StatusPagamento.PAGO;
        this.pagoEm = quando;
    }

    public void recusar(String motivo) {
        exigirStatus("recusar", StatusPagamento.PENDENTE, StatusPagamento.AUTORIZADO);
        this.status = StatusPagamento.RECUSADO;
        this.motivoRecusa = motivo;
    }

    public void expirar() {
        exigirStatus("expirar", StatusPagamento.AGUARDANDO_PAGAMENTO);
        this.status = StatusPagamento.EXPIRADO;
    }

    public void cancelar() {
        exigirStatus("cancelar", StatusPagamento.PENDENTE, StatusPagamento.AGUARDANDO_PAGAMENTO,
                StatusPagamento.AUTORIZADO);
        this.status = StatusPagamento.CANCELADO;
        this.canceladoEm = LocalDateTime.now();
    }

    public void estornar() {
        exigirStatus("estornar", StatusPagamento.PAGO);
        this.pedido.cancelar();
        this.status = StatusPagamento.ESTORNADO;
    }

    public void contestar() {
        exigirStatus("contestar", StatusPagamento.PAGO);
        this.status = StatusPagamento.CONTESTADO;
    }

    /** Guarda o que o gateway devolveu ao criar a cobrança. */
    public void registrarCobranca(String gatewayPaymentId, DadosCartao cartao, DadosPix pix, DadosBoleto boleto) {
        this.gatewayPaymentId = gatewayPaymentId;
        this.cartao = cartao;
        this.pix = pix;
        this.boleto = boleto;
    }

    public void adicionarTransacao(TransacaoGateway transacao) {
        this.transacoes.add(transacao);
    }

    private void exigirStatus(String acao, StatusPagamento... permitidos) {
        if (!Arrays.asList(permitidos).contains(this.status)) {
            throw new BusinessRuleViolationException(
                    "Não é possível " + acao + " um pagamento com status " + this.status + ".", "status");
        }
    }

    public Pedido getPedido() {
        return pedido;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getGateway() {
        return gateway;
    }

    public String getGatewayPaymentId() {
        return gatewayPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public DadosCartao getCartao() {
        return cartao;
    }

    public DadosPix getPix() {
        return pix;
    }

    public DadosBoleto getBoleto() {
        return boleto;
    }

    public List<TransacaoGateway> getTransacoes() {
        return transacoes;
    }
}
