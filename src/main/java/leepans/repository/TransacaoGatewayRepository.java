package leepans.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import leepans.model.TransacaoGateway;

@ApplicationScoped
public class TransacaoGatewayRepository implements PanacheRepository<TransacaoGateway> {

    public List<TransacaoGateway> findByPagamentoId(Long pagamentoId) {
        return list("pagamento.id = ?1 ORDER BY id", pagamentoId);
    }
}
