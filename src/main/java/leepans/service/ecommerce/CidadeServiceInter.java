package leepans.service.ecommerce;

import java.util.List;

import leepans.dto.cidade.CidadeRequestDTO;
import leepans.model.Cidade;

public interface CidadeServiceInter {

    List<Cidade> findAll();

    Cidade findById(Long id);

    List<Cidade> findByNome(String nome);

    List<Cidade> findByEstadoId(Long idEstado);

    Cidade create(Cidade cidade);

    void update(Long id, CidadeRequestDTO dto);

    void delete(Long id);
}

