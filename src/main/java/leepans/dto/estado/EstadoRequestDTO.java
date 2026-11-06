package leepans.dto.estado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EstadoRequestDTO(
        @NotBlank(message = "O nome do estado é obrigatório")
        @Size(min = 2, max = 100, message = "O nome do estado deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "A sigla do estado é obrigatória")
        @Size(min = 2, max = 2, message = "A sigla do estado deve ter 2 caracteres")
        String sigla,

        @NotNull(message = "O ID da região é obrigatório")
        Long idRegiao,

        Integer version
) {
}

