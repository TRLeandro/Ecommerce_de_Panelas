package leepans.service.ecommerce;

import java.util.List;

import leepans.dto.regiao.RegiaoRequestDTO;
import leepans.model.Regiao;

public interface RegiaoServiceInter {

    List<Regiao> findAll();

    Regiao findById(Long id);

    List<Regiao> findByNome(String nome);

    Regiao create(Regiao regiao);

    void update(Long id, RegiaoRequestDTO dto);

    void delete(Long id);
}

