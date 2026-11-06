package leepans.dto.regiao;

import java.time.LocalDateTime;

public record RegiaoResponseDTO(
        Long id,
        String nome,
        LocalDateTime dataCadastro,
        Integer version
) {
}

