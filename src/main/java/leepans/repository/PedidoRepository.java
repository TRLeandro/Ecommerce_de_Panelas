package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.Pedido;
import leepans.model.StatusPedido;

@ApplicationScoped
public class PedidoRepository implements PanacheRepository<Pedido> {



    public PanacheQuery<Pedido> findByUsuarioId(Long usuarioId) {
        return find("SELECT p FROM Pedido p WHERE p.usuario.id = ?1", usuarioId);
    }

    public PanacheQuery<Pedido> findByStatus(StatusPedido status) {
        return find("SELECT p FROM Pedido p WHERE p.status = ?1", status);
    }

    public PanacheQuery<Pedido> findByEnderecoCidade(String cidade) {
        return find("SELECT p FROM Pedido p WHERE UPPER(p.endereco.cidade.nome) LIKE UPPER(?1)", "%" + cidade + "%");
    }

    public PanacheQuery<Pedido> findComprasUsuario(String usuarioLogin) {
        return find(
            "SELECT p FROM Pedido p " +
            "LEFT JOIN FETCH p.pagamento " +
            "LEFT JOIN FETCH p.itens " +
            "WHERE p.usuario.login = ?1 " +
            "ORDER BY p.dataCadastro DESC",
            usuarioLogin
        );
    }

    public PanacheQuery<Pedido> findComprasUsuario(String usuarioLogin, StatusPedido status) {
        return find(
            "SELECT p FROM Pedido p " +
            "LEFT JOIN FETCH p.pagamento " +
            "LEFT JOIN FETCH p.itens " +
            "WHERE p.usuario.login = ?1 AND p.status = ?2 " +
            "ORDER BY p.dataCadastro DESC",
            usuarioLogin, status
        );
    }
}
