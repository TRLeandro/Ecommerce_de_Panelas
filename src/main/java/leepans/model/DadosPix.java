package leepans.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DadosPix {

    @Column(name = "pix_qr_code_copia_e_cola", columnDefinition = "TEXT")
    private String qrCodeCopiaECola;

    @Column(name = "pix_qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "pix_txid")
    private String txid;

    public DadosPix() {
    }

    public DadosPix(String qrCodeCopiaECola, String qrCodeBase64, String txid) {
        this.qrCodeCopiaECola = qrCodeCopiaECola;
        this.qrCodeBase64 = qrCodeBase64;
        this.txid = txid;
    }

    public String getQrCodeCopiaECola() {
        return qrCodeCopiaECola;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public String getTxid() {
        return txid;
    }
}
