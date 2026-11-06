package leepans.repository;

import java.math.BigDecimal;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import leepans.model.CupomDesconto;

@ApplicationScoped
public class CupomDescontoRepository implements PanacheRepository<CupomDesconto> {

    public PanacheQuery<CupomDesconto> findByAtivo(boolean ativo) {
        return find("ativo", ativo);
    }

    public PanacheQuery<CupomDesconto> findByCodigo(String codigo) {
        return find("SELECT c FROM CupomDesconto c WHERE UPPER(c.codigo) LIKE UPPER(?1)", "%" + codigo + "%");
    }

    /**
     * Busca pelo código exato travando a linha até o fim da transação, para que
     * compras simultâneas não consumam o mesmo saldo do cupom.
     */
    public CupomDesconto findByCodigoParaAtualizacao(String codigo) {
        return find("UPPER(codigo) = UPPER(?1)", codigo)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
    }

    public PanacheQuery<CupomDesconto> findByAtivoAndValorMinimoCompra(boolean ativo, BigDecimal valorMinimo) {
        return find("ativo = ?1 and valorMinimoCompra <= ?2", ativo, valorMinimo);
    }
}
