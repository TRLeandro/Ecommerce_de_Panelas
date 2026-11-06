package leepans.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDTO(
        @NotBlank(message = "O logradouro é obrigatório")
        @Size(min = 3, max = 100, message = "O logradouro deve ter entre 3-100 caracteres")
        String logradouro,

        @NotBlank(message = "O número é obrigatório")
        @Size(min = 1, max = 10, message = "O número deve ter entre 1-10 caracteres")
        String numero,

        String complemento,

        @NotBlank(message = "O bairro é obrigatório")
        @Size(min = 2, max = 100, message = "O bairro deve ter entre 2-100 caracteres")
        String bairro,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "O CEP deve estar no formato XXXXX-XXX")
        String cep,

        @NotNull(message = "A cidade é obrigatória")
        Long idCidade,

        Integer version
) {
}
