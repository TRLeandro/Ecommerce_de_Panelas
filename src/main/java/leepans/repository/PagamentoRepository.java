package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Pagamento;
import leepans.model.StatusPagamento;

@ApplicationScoped
public class PagamentoRepository implements PanacheRepository<Pagamento> {

    public Pagamento findByIdDoUsuario(Long id, String login) {
        return find("id = ?1 AND pedido.usuario.login = ?2", id, login).firstResult();
    }

    public PanacheQuery<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento) {
        return find("status", statusPagamento);
    }

    public PanacheQuery<Pagamento> findByUsuario(String login) {
        return find("SELECT p FROM Pagamento p " +
                "WHERE p.pedido.usuario.login = ?1", login);
    }
}
