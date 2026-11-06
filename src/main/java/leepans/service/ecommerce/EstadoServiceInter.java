package leepans.service.ecommerce;

import java.util.List;

import leepans.dto.estado.EstadoRequestDTO;
import leepans.model.Estado;

public interface EstadoServiceInter {

    List<Estado> findAll();

    Estado findById(Long id);

    List<Estado> findByNome(String nome);

    Estado findBySigla(String sigla);

    List<Estado> findByRegiaoId(Long idRegiao);

    Estado create(Estado estado);

    void update(Long id, EstadoRequestDTO dto);

    void delete(Long id);
}

