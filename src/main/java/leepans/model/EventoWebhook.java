package leepans.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Registro de cada evento recebido do gateway. A constraint única garante que
 * um evento repetido seja reconhecido; {@code processado} separa o evento já
 * aplicado daquele cuja tentativa anterior falhou e deve ser refeita.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_evento_webhook_gateway_evento",
        columnNames = { "gateway", "evento_id" }))
public class EventoWebhook extends DefaultEntity {

    @Column(name = "gateway", nullable = false)
    private String gateway;

    @Column(name = "evento_id", nullable = false)
    private String eventoId;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "processado", nullable = false)
    private boolean processado;

    @Column(name = "payload_bruto", columnDefinition = "TEXT")
    private String payloadBruto;

    protected EventoWebhook() {
    }

    public EventoWebhook(String gateway, String eventoId, String payloadBruto) {
        this.gateway = gateway;
        this.eventoId = eventoId;
        this.payloadBruto = TransacaoGateway.mascarar(payloadBruto);
        this.recebidoEm = LocalDateTime.now();
    }

    public void marcarComoProcessado() {
        this.processado = true;
    }

    public String getGateway() {
        return gateway;
    }

    public String getEventoId() {
        return eventoId;
    }

    public LocalDateTime getRecebidoEm() {
        return recebidoEm;
    }

    public boolean isProcessado() {
        return processado;
    }

    public String getPayloadBruto() {
        return payloadBruto;
    }
}
