package leepans.dto.endereco;

import java.time.LocalDateTime;

import leepans.dto.cidade.CidadeResponseDTO;

public record EnderecoResponseDTO(
        Long id,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cep,
        CidadeResponseDTO cidade,
        LocalDateTime dataCadastro,
        Integer version
) {
}
