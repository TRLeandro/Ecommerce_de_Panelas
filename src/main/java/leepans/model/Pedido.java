package leepans.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import leepans.exception.BusinessRuleViolationException;

/**
 * O status só muda pelos métodos de transição desta classe; cada um rejeita a
 * mudança se ela não for válida.
 */
@Entity
public class Pedido extends DefaultEntity {

    private static final List<StatusPedido> STATUS_CANCELAVEIS = List.of(
            StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.PAGO, StatusPedido.EM_SEPARACAO);

    @ManyToOne
    private Usuario usuario;

    @Embedded
    private EnderecoEntrega enderecoEntrega;

    @Column(name = "codigo_status_pedido", nullable = false)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @Column(name = "valor_itens", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorItens;

    @Column(name = "valor_desconto", precision = 19, scale = 2)
    private BigDecimal valorDesconto;

    // TODO frete: sem cálculo por enquanto, o valor fica sempre em 0.00
    @Column(name = "valor_frete", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorFrete = new BigDecimal("0.00");

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ItemPedido> itens;

    @ManyToOne
    @JoinColumn(name = "cupom_desconto_id")
    private CupomDesconto cupomDesconto;

    @OneToMany(mappedBy = "pedido")
    @OrderBy("id")
    private List<Pagamento> pagamentos = new ArrayList<>();

    /**
     * Calcula o valor final líquido que deve ser cobrado no pagamento.
     */
    public BigDecimal valorTotal() {
        BigDecimal desconto = this.valorDesconto != null ? this.valorDesconto : BigDecimal.ZERO;
        return this.valorItens.subtract(desconto).add(this.valorFrete);
    }

    public Optional<Pagamento> pagamentoAtivo() {
        return this.pagamentos.stream().filter(pagamento -> pagamento.getStatus().isAtivo()).findFirst();
    }

    public Optional<Pagamento> pagamentoAprovado() {
        return this.pagamentos.stream().filter(pagamento -> pagamento.getStatus() == StatusPagamento.PAGO).findFirst();
    }

    public void marcarComoPago() {
        exigirStatus("marcar como pago", StatusPedido.AGUARDANDO_PAGAMENTO);
        this.status = StatusPedido.PAGO;
    }

    public void iniciarSeparacao() {
        exigirStatus("iniciar a separação de", StatusPedido.PAGO);
        this.status = StatusPedido.EM_SEPARACAO;
    }

    public void marcarComoEnviado() {
        exigirStatus("marcar como enviado", StatusPedido.EM_SEPARACAO);
        this.status = StatusPedido.ENVIADO;
    }

    public void marcarComoEntregue() {
        exigirStatus("marcar como entregue", StatusPedido.ENVIADO);
        this.status = StatusPedido.ENTREGUE;
    }

    public boolean podeCancelar() {
        return STATUS_CANCELAVEIS.contains(this.status);
    }

    public void cancelar() {
        exigirStatus("cancelar", STATUS_CANCELAVEIS.toArray(StatusPedido[]::new));
        this.status = StatusPedido.CANCELADO;
    }

    private void exigirStatus(String acao, StatusPedido... permitidos) {
        if (!Arrays.asList(permitidos).contains(this.status)) {
            throw new BusinessRuleViolationException(
                    "Não é possível " + acao + " um pedido com status " + this.status + ".", "status");
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EnderecoEntrega getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public void setEnderecoEntrega(EnderecoEntrega enderecoEntrega) {
        this.enderecoEntrega = enderecoEntrega;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getValorItens() {
        return valorItens;
    }

    public void setValorItens(BigDecimal valorItens) {
        this.valorItens = valorItens;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public CupomDesconto getCupomDesconto() {
        return cupomDesconto;
    }

    public void setCupomDesconto(CupomDesconto cupomDesconto) {
        this.cupomDesconto = cupomDesconto;
    }

    public List<Pagamento> getPagamentos() {
        return pagamentos;
    }
}
