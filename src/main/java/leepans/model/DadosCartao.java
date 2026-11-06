package leepans.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Únicos dados de cartão que a loja guarda. Número, CVV e validade nunca chegam
 * aqui: a API recebe só o token gerado pelo SDK do gateway no front-end.
 */
@Embeddable
public class DadosCartao {

    @Column(name = "cartao_bandeira")
    private String bandeira;

    @Column(name = "cartao_ultimos4_digitos", length = 4)
    private String ultimos4Digitos;

    @Column(name = "cartao_parcelas")
    private Integer parcelas;

    @Column(name = "cartao_codigo_autorizacao")
    private String codigoAutorizacao;

    public DadosCartao() {
    }

    public DadosCartao(String bandeira, String ultimos4Digitos, Integer parcelas, String codigoAutorizacao) {
        this.bandeira = bandeira;
        this.ultimos4Digitos = ultimos4Digitos;
        this.parcelas = parcelas;
        this.codigoAutorizacao = codigoAutorizacao;
    }

    public String getBandeira() {
        return bandeira;
    }

    public String getUltimos4Digitos() {
        return ultimos4Digitos;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public String getCodigoAutorizacao() {
        return codigoAutorizacao;
    }

    public void setCodigoAutorizacao(String codigoAutorizacao) {
        this.codigoAutorizacao = codigoAutorizacao;
    }
}
