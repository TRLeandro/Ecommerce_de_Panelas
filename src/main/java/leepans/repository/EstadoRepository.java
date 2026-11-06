package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Estado;

@ApplicationScoped
public class EstadoRepository implements PanacheRepository<Estado> {

    public PanacheQuery<Estado> findByNome(String nome) {
        return find("UPPER(nome) LIKE UPPER(?1)", "%" + nome + "%");
    }

    public PanacheQuery<Estado> findBySigla(String sigla) {
        return find("UPPER(sigla) = UPPER(?1)", sigla);
    }

    public PanacheQuery<Estado> findByRegiaoId(Long idRegiao) {
        return find("regiao.id = ?1", idRegiao);
    }
}

