package leepans.dto.cidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CidadeRequestDTO(
        @NotBlank(message = "O nome da cidade é obrigatório")
        @Size(min = 2, max = 100, message = "O nome da cidade deve ter entre 2 e 100 caracteres")
        String nome,

        @NotNull(message = "O ID do estado é obrigatório")
        Long idEstado,

        Integer version
) {
}

