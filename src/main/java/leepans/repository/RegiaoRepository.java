package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Regiao;

@ApplicationScoped
public class RegiaoRepository implements PanacheRepository<Regiao> {

    public PanacheQuery<Regiao> findByNome(String nome) {
        return find("UPPER(nome) LIKE UPPER(?1)", "%" + nome + "%");
    }
}

