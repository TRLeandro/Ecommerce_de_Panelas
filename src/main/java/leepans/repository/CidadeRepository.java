package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Cidade;

@ApplicationScoped
public class CidadeRepository implements PanacheRepository<Cidade> {

    public PanacheQuery<Cidade> findByNome(String nome) {
        return find("UPPER(nome) LIKE UPPER(?1)", "%" + nome + "%");
    }

    public PanacheQuery<Cidade> findByEstadoId(Long idEstado) {
        return find("estado.id = ?1", idEstado);
    }
}

