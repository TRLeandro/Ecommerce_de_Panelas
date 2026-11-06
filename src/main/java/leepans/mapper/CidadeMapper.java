package leepans.mapper;

import leepans.dto.cidade.CidadeRequestDTO;
import leepans.dto.cidade.CidadeResponseDTO;
import leepans.model.Cidade;
import leepans.model.Estado;

public class CidadeMapper {

    public static Cidade toEntity(CidadeRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Cidade cidade = new Cidade();
        cidade.setNome(dto.nome());
        if (dto.idEstado() != null) {
            Estado estado = new Estado();
            estado.setId(dto.idEstado());
            cidade.setEstado(estado);
        }
        if (dto.version() != null) {
            cidade.setVersion(dto.version());
        }

        return cidade;
    }

    public static CidadeResponseDTO toResponseDTO(Cidade cidade) {
        if (cidade == null) {
            return null;
        }

        return new CidadeResponseDTO(
                cidade.getId(),
                cidade.getNome(),
                EstadoMapper.toResponseDTO(cidade.getEstado()),
                cidade.getDataCadastro(),
                cidade.getVersion());
    }
}

