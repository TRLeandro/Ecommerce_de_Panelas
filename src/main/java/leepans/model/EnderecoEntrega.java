package leepans.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Cópia do endereço no momento da compra. Guarda texto, não referências: editar
 * o Endereco ou a Cidade depois não altera pedidos antigos.
 */
@Embeddable
public class EnderecoEntrega {

    @Column(name = "entrega_logradouro")
    private String logradouro;

    @Column(name = "entrega_numero")
    private String numero;

    @Column(name = "entrega_complemento")
    private String complemento;

    @Column(name = "entrega_bairro")
    private String bairro;

    @Column(name = "entrega_cidade")
    private String cidade;

    @Column(name = "entrega_estado")
    private String estado;

    @Column(name = "entrega_cep")
    private String cep;

    public EnderecoEntrega() {
    }

    public EnderecoEntrega(String logradouro, String numero, String complemento, String bairro,
            String cidade, String estado, String cep) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.cep = cep;
    }

    public static EnderecoEntrega copiaDe(Endereco endereco, Cidade cidade) {
        return new EnderecoEntrega(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                cidade.getNome(),
                cidade.getEstado().getSigla(),
                endereco.getCep());
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public String getCep() {
        return cep;
    }
}
