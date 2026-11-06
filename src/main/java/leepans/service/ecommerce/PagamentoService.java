package leepans.service.ecommerce;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import leepans.exception.ResourceNotFoundException;
import leepans.model.Pagamento;
import leepans.model.StatusPagamento;
import leepans.repository.PagamentoRepository;

@ApplicationScoped
public class PagamentoService implements PagamentoServiceInter {

    @Inject
    PagamentoRepository repository;

    @Override
    public List<Pagamento> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Pagamento findById(Long id) {
        Pagamento pagamento = repository.findById(id);
        if (pagamento == null) {
            throw new ResourceNotFoundException("Pagamento", id);
        }
        return pagamento;
    }

    @Override
    public Pagamento findByIdDoUsuario(Long id, String login) {
        Pagamento pagamento = repository.findByIdDoUsuario(id, login);
        if (pagamento == null) {
            // Mesma resposta para "não existe" e "é de outro usuário": não revela a existência do ID.
            throw new ResourceNotFoundException("Pagamento", id);
        }
        return pagamento;
    }

    @Override
    public List<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento) {
        return repository.findByStatusPagamento(statusPagamento).list();
    }

    @Override
    public List<Pagamento> findByUsuario(String login) {
        return repository.findByUsuario(login).list();
    }
}
