package leepans.dto.regiao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegiaoRequestDTO(
        @NotBlank(message = "O nome da região é obrigatório")
        @Size(min = 2, max = 50, message = "O nome da região deve ter entre 2 e 50 caracteres")
        String nome,

        Integer version
) {
}

