package leepans.service.ecommerce;

import java.math.BigDecimal;
import java.util.List;

import leepans.dto.cupomDesconto.CupomDescontoRequestDTO;
import leepans.model.CupomDesconto;

public interface CupomDescontoServiceInter {

    List<CupomDesconto> findAll();

    CupomDesconto findById(Long id);

    List<CupomDesconto> findByAtivo(boolean ativo);

    CupomDesconto findByCodigo(String codigo);

    List<CupomDesconto> findByAtivoAndValorMinimoCompra(boolean ativo, BigDecimal valorMinimo);

    CupomDesconto create(CupomDesconto cupomDesconto);

    void update(Long id, CupomDescontoRequestDTO dto);

    void changeStatus(Long id);

    void incrementarQuantidade(CupomDesconto cupomDesconto);

    void delete(Long id);
}
