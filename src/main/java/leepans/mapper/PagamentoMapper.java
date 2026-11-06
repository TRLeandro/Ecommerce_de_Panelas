package leepans.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import leepans.dto.pagamento.BoletoResponseDTO;
import leepans.dto.pagamento.CartaoResponseDTO;
import leepans.dto.pagamento.PagamentoEcommerceDTO;
import leepans.dto.pagamento.PagamentoResponseDTO;
import leepans.dto.pagamento.PixResponseDTO;
import leepans.model.DadosBoleto;
import leepans.model.DadosCartao;
import leepans.model.DadosPix;
import leepans.model.Pagamento;

@ApplicationScoped
public class PagamentoMapper {

    public PagamentoResponseDTO toResponse(Pagamento pagamento) {
        if (pagamento == null)
            return null;

        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getPedido().getId(),
                pagamento.getMetodo(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pagamento.getGateway(),
                pagamento.getGatewayPaymentId(),
                pagamento.getMotivoRecusa(),
                pagamento.getExpiraEm(),
                pagamento.getPagoEm(),
                pagamento.getCanceladoEm(),
                toResponse(pagamento.getCartao()),
                toResponse(pagamento.getPix()),
                toResponse(pagamento.getBoleto()),
                pagamento.getDataCadastro(),
                pagamento.getVersion());
    }

    public PagamentoEcommerceDTO toEcommerceDTO(Pagamento pagamento) {
        if (pagamento == null)
            return null;

        return new PagamentoEcommerceDTO(
                pagamento.getId(),
                pagamento.getPedido().getId(),
                pagamento.getMetodo(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pagamento.getPagoEm());
    }

    private CartaoResponseDTO toResponse(DadosCartao cartao) {
        if (cartao == null)
            return null;

        return new CartaoResponseDTO(cartao.getBandeira(), cartao.getUltimos4Digitos(), cartao.getParcelas());
    }

    private PixResponseDTO toResponse(DadosPix pix) {
        if (pix == null)
            return null;

        return new PixResponseDTO(pix.getQrCodeCopiaECola(), pix.getQrCodeBase64());
    }

    private BoletoResponseDTO toResponse(DadosBoleto boleto) {
        if (boleto == null)
            return null;

        return new BoletoResponseDTO(boleto.getLinhaDigitavel(), boleto.getUrlPdf(), boleto.getDataVencimento());
    }
}
