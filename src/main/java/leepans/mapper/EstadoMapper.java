package leepans.mapper;

import leepans.dto.estado.EstadoRequestDTO;
import leepans.dto.estado.EstadoResponseDTO;
import leepans.model.Estado;
import leepans.model.Regiao;

public class EstadoMapper {

    public static Estado toEntity(EstadoRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Estado estado = new Estado();
        estado.setNome(dto.nome());
        estado.setSigla(dto.sigla());
        if (dto.idRegiao() != null) {
            Regiao regiao = new Regiao();
            regiao.setId(dto.idRegiao());
            estado.setRegiao(regiao);
        }
        if (dto.version() != null) {
            estado.setVersion(dto.version());
        }

        return estado;
    }

    public static EstadoResponseDTO toResponseDTO(Estado estado) {
        if (estado == null) {
            return null;
        }

        return new EstadoResponseDTO(
                estado.getId(),
                estado.getNome(),
                estado.getSigla(),
                RegiaoMapper.toResponseDTO(estado.getRegiao()),
                estado.getDataCadastro(),
                estado.getVersion());
    }
}

