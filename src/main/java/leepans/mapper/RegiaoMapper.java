package leepans.mapper;

import leepans.dto.regiao.RegiaoRequestDTO;
import leepans.dto.regiao.RegiaoResponseDTO;
import leepans.model.Regiao;

public class RegiaoMapper {

    public static Regiao toEntity(RegiaoRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Regiao regiao = new Regiao();
        regiao.setNome(dto.nome());
        if (dto.version() != null) {
            regiao.setVersion(dto.version());
        }

        return regiao;
    }

    public static RegiaoResponseDTO toResponseDTO(Regiao regiao) {
        if (regiao == null) {
            return null;
        }

        return new RegiaoResponseDTO(
                regiao.getId(),
                regiao.getNome(),
                regiao.getDataCadastro(),
                regiao.getVersion());
    }
}

