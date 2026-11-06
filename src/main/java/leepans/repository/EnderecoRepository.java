package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Endereco;

@ApplicationScoped
public class EnderecoRepository implements PanacheRepository<Endereco> {

    public PanacheQuery<Endereco> findByUsuario(String login) {
        return find("SELECT e FROM Endereco e "+
        "INNER JOIN Usuario u ON u.endereco.id = e.id "+
        "WHERE u.login = ?1", login);
    }

    public PanacheQuery<Endereco> findByCidade(String cidade) {
        return find("SELECT e FROM Endereco e WHERE UPPER(e.cidade.nome) LIKE UPPER(?1)", "%" + cidade + "%");
    }

    public PanacheQuery<Endereco> findByEstado(String estado) {
        return find("SELECT e FROM Endereco e WHERE UPPER(e.cidade.estado.nome) LIKE UPPER(?1) OR UPPER(e.cidade.estado.sigla) LIKE UPPER(?1)", "%" + estado + "%");
    }

    public Endereco findByAllFields(String logradouro, String numero, String complemento, String bairro, String cep, Long idCidade) {
        if (idCidade == null) {
            return null;
        }
        if (complemento == null || complemento.isBlank()) {
            return find("logradouro = ?1 AND numero = ?2 AND (complemento IS NULL OR complemento = '') AND bairro = ?3 AND cep = ?4 AND cidade.id = ?5",
                    logradouro, numero, bairro, cep, idCidade).firstResult();
        }
        return find("logradouro = ?1 AND numero = ?2 AND complemento = ?3 AND bairro = ?4 AND cep = ?5 AND cidade.id = ?6",
                logradouro, numero, complemento, bairro, cep, idCidade).firstResult();
    }
}
