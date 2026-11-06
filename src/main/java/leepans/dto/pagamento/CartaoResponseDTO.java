package leepans.dto.pagamento;

public record CartaoResponseDTO(
        String bandeira,
        String ultimos4Digitos,
        Integer parcelas
) {
}
