package leepans.repository;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import leepans.model.Pedido;
import leepans.model.StatusPagamento;
import leepans.model.StatusPedido;

@ApplicationScoped
public class PedidoRepository implements PanacheRepository<Pedido> {


    public Pedido findByIdDoUsuario(Long id, String login) {
        return find("id = ?1 AND usuario.login = ?2", id, login).firstResult();
    }

    /**
     * Trava a linha do pedido até o fim da transação e recarrega seu estado. É o
     * que serializa as operações de pagamento de um mesmo pedido.
     */
    public void travar(Pedido pedido) {
        getEntityManager().refresh(pedido, LockModeType.PESSIMISTIC_WRITE);
    }

    /** Pedidos sem pagamento em andamento que continuam aguardando desde antes do limite. */
    public List<Long> findIdsAbandonados(LocalDateTime limite) {
        return getEntityManager().createQuery(
                "SELECT p.id FROM Pedido p WHERE p.status = :status AND p.dataCadastro < :limite "
                        + "AND NOT EXISTS (SELECT 1 FROM Pagamento g WHERE g.pedido = p AND g.status IN :ativos)",
                Long.class)
                .setParameter("status", StatusPedido.AGUARDANDO_PAGAMENTO)
                .setParameter("limite", limite)
                .setParameter("ativos", List.of(StatusPagamento.PENDENTE, StatusPagamento.AGUARDANDO_PAGAMENTO,
                        StatusPagamento.AUTORIZADO))
                .getResultList();
    }

    public PanacheQuery<Pedido> findByUsuarioId(Long usuarioId) {
        return find("SELECT p FROM Pedido p WHERE p.usuario.id = ?1", usuarioId);
    }

    public PanacheQuery<Pedido> findByStatus(StatusPedido status) {
        return find("SELECT p FROM Pedido p WHERE p.status = ?1", status);
    }

    public PanacheQuery<Pedido> findByEnderecoCidade(String cidade) {
        return find("SELECT p FROM Pedido p WHERE UPPER(p.enderecoEntrega.cidade) LIKE UPPER(?1)", "%" + cidade + "%");
    }

    public PanacheQuery<Pedido> findComprasUsuario(String usuarioLogin) {
        return find(
            "SELECT p FROM Pedido p " +
            "LEFT JOIN FETCH p.itens " +
            "WHERE p.usuario.login = ?1 " +
            "ORDER BY p.dataCadastro DESC",
            usuarioLogin
        );
    }

    public PanacheQuery<Pedido> findComprasUsuario(String usuarioLogin, StatusPedido status) {
        return find(
            "SELECT p FROM Pedido p " +
            "LEFT JOIN FETCH p.itens " +
            "WHERE p.usuario.login = ?1 AND p.status = ?2 " +
            "ORDER BY p.dataCadastro DESC",
            usuarioLogin, status
        );
    }
}
