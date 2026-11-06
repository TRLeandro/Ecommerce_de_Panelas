package leepans.mapper;

import leepans.dto.endereco.EnderecoRequestDTO;
import leepans.dto.endereco.EnderecoResponseDTO;
import leepans.model.Cidade;
import leepans.model.Endereco;

public class EnderecoMapper {

    public static Endereco toEntity(EnderecoRequestDTO dto) {
        if (dto == null) return null;

        Endereco endereco = new Endereco();
        endereco.setLogradouro(dto.logradouro());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setBairro(dto.bairro());
        endereco.setCep(dto.cep());
        if (dto.idCidade() != null) {
            Cidade cidade = new Cidade();
            cidade.setId(dto.idCidade());
            endereco.setCidade(cidade);
        }
        if (dto.version() != null) {
            endereco.setVersion(dto.version());
        }
        return endereco;
    }

    public static EnderecoResponseDTO toResponse(Endereco endereco) {
        if (endereco == null) return null;

        return new EnderecoResponseDTO(
                endereco.getId(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCep(),
                CidadeMapper.toResponseDTO(endereco.getCidade()),
                endereco.getDataCadastro(),
                endereco.getVersion()
        );
    }
}
