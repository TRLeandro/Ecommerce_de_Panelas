package leepans.service.ecommerce;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import leepans.dto.cupomDesconto.CupomDescontoRequestDTO;
import leepans.model.CupomDesconto;
import leepans.repository.CupomDescontoRepository;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class CupomDescontoService implements CupomDescontoServiceInter {

    @Inject
    CupomDescontoRepository repository;

    @Override
    public List<CupomDesconto> findAll() {
        return repository.findAll().list();
    }

    @Override
    public CupomDesconto findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<CupomDesconto> findByAtivo(boolean ativo) {
        return repository.findByAtivo(ativo).list();
    }

    @Override
    public CupomDesconto findByCodigo(String codigo) {
        return repository.findByCodigo(codigo).firstResult();
    }

    @Override
    public List<CupomDesconto> findByAtivoAndValorMinimoCompra(boolean ativo, BigDecimal valorMinimo) {
        return repository.findByAtivoAndValorMinimoCompra(ativo, valorMinimo).list();
    }

    @Override
    @Transactional
    public CupomDesconto create(CupomDesconto cupomDesconto) {
        repository.persist(cupomDesconto);
        return cupomDesconto;
    }

    @Override
    @Transactional
    public void update(Long id, CupomDescontoRequestDTO dto) {
        CupomDesconto cupomDesconto = repository.findById(id);

        if (cupomDesconto.getVersion() != dto.version()) {
            throw new OptimisticLockException(
                    "Conflito de concorrência: o cupom de desconto foi alterado por outra transação."
            );
        }

        cupomDesconto.setCodigo(dto.codigo());
        cupomDesconto.setValorDesconto(dto.valorDesconto());
        cupomDesconto.setPercentualDesconto(dto.percentualDesconto());
        cupomDesconto.setValorMinimoCompra(dto.valorMinimoCompra());
        cupomDesconto.setDataValidade(dto.dataValidade());
        cupomDesconto.setQuantidadeDisponivel(dto.quantidadeDisponivel());
        cupomDesconto.setAtivo(dto.ativo());
        repository.persist(cupomDesconto);
    }

    @Override
    @Transactional
    public void changeStatus(Long id) {
        CupomDesconto cupom = repository.findById(id);
        if(cupom.isAtivo())
            cupom.setAtivo(false);
        else
            cupom.setAtivo(true);
        repository.persist(cupom);

    }

    /** Devolve o uso do cupom. Quantidade nula significa cupom sem limite de usos. */
    @Transactional
    public void incrementarQuantidade(CupomDesconto cupomDesconto) {
        repository.getEntityManager().refresh(cupomDesconto, LockModeType.PESSIMISTIC_WRITE);
        if (cupomDesconto.getQuantidadeDisponivel() != null) {
            cupomDesconto.setQuantidadeDisponivel(cupomDesconto.getQuantidadeDisponivel() + 1);
        }
    }

    @Transactional
    public void desativarSeEsgotado(Long id) {
        CupomDesconto cupom = repository.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (cupom.getQuantidadeDisponivel() != null && cupom.getQuantidadeDisponivel() == 0) {
            cupom.setAtivo(false);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
