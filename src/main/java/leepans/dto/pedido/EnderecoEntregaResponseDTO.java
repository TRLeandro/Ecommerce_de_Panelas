package leepans.dto.pedido;

public record EnderecoEntregaResponseDTO(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep
) {
}
