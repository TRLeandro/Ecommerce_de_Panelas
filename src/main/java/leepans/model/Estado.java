package leepans.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Estado extends DefaultEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 2)
    private String sigla;

    @ManyToOne
    @JoinColumn(name = "id_regiao", nullable = false)
    private Regiao regiao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }
}

