package leepans.dto.estado;

import java.time.LocalDateTime;

import leepans.dto.regiao.RegiaoResponseDTO;

public record EstadoResponseDTO(
        Long id,
        String nome,
        String sigla,
        RegiaoResponseDTO regiao,
        LocalDateTime dataCadastro,
        Integer version
) {
}

