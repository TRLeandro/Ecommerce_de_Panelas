package leepans.service.ecommerce;

import java.util.List;

import leepans.dto.pagamento.BoletoRequestDTO;
import leepans.dto.pagamento.CartaoRequestDTO;
import leepans.dto.pagamento.PagamentoPatchDTO;
import leepans.dto.pagamento.PixRequestDTO;
import leepans.model.Pagamento;
import leepans.model.StatusPagamento;

public interface PagamentoServiceInter {

    List<Pagamento> findAll();

    Pagamento findById(Long id);

    Pagamento findByIdDoUsuario(Long id, String login);

    List<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento);

    List<Pagamento> findByUsuario(String login);

    Pagamento create(Pagamento pagamento);

    void processarPagamento(Pagamento pagamento);

    void completeInfo(Long id, CartaoRequestDTO dto);

    void completeInfo(Long id, BoletoRequestDTO dto);

    void completeInfo(Long id, PixRequestDTO dto);

    void setStatus(Long id, PagamentoPatchDTO dto);

    void delete(Long id);
}
