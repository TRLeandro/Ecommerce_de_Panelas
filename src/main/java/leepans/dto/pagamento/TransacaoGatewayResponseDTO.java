package leepans.dto.pagamento;

import java.time.LocalDateTime;

import leepans.model.TipoTransacao;

public record TransacaoGatewayResponseDTO(
        Long id,
        TipoTransacao tipo,
        String statusGateway,
        String codigoRetorno,
        String mensagem,
        String payloadBruto,
        LocalDateTime dataCadastro
) {
}
