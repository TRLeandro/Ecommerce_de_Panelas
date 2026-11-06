package leepans.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DadosBoleto {

    @Column(name = "boleto_linha_digitavel")
    private String linhaDigitavel;

    @Column(name = "boleto_codigo_barras")
    private String codigoBarras;

    @Column(name = "boleto_url_pdf")
    private String urlPdf;

    @Column(name = "boleto_data_vencimento")
    private LocalDate dataVencimento;

    public DadosBoleto() {
    }

    public DadosBoleto(String linhaDigitavel, String codigoBarras, String urlPdf, LocalDate dataVencimento) {
        this.linhaDigitavel = linhaDigitavel;
        this.codigoBarras = codigoBarras;
        this.urlPdf = urlPdf;
        this.dataVencimento = dataVencimento;
    }

    public String getLinhaDigitavel() {
        return linhaDigitavel;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }
}
