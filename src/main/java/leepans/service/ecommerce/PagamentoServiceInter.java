package leepans.service.ecommerce;

import java.util.List;

import leepans.model.Pagamento;
import leepans.model.StatusPagamento;

public interface PagamentoServiceInter {

    List<Pagamento> findAll();

    Pagamento findById(Long id);

    Pagamento findByIdDoUsuario(Long id, String login);

    List<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento);

    List<Pagamento> findByUsuario(String login);
}
