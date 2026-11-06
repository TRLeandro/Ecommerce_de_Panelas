package leepans.dto.pagamento;

import java.time.LocalDate;

public record BoletoResponseDTO(
        String linhaDigitavel,
        String urlPdf,
        LocalDate dataVencimento
) {
}
