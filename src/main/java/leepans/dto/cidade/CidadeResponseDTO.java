package leepans.dto.cidade;

import java.time.LocalDateTime;

import leepans.dto.estado.EstadoResponseDTO;

public record CidadeResponseDTO(
        Long id,
        String nome,
        EstadoResponseDTO estado,
        LocalDateTime dataCadastro,
        Integer version
) {
}

