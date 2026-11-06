package leepans.service.ecommerce;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import leepans.dto.regiao.RegiaoRequestDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.model.Regiao;
import leepans.repository.RegiaoRepository;

@ApplicationScoped
public class RegiaoService implements RegiaoServiceInter {

    @Inject
    RegiaoRepository repository;

    @Override
    public List<Regiao> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Regiao findById(Long id) {
        Regiao regiao = repository.findById(id);
        if (regiao == null) {
            throw new ResourceNotFoundException("Região", id);
        }
        return regiao;
    }

    @Override
    public List<Regiao> findByNome(String nome) {
        return repository.findByNome(nome).list();
    }

    @Override
    @Transactional
    public Regiao create(Regiao regiao) {
        repository.persist(regiao);
        return regiao;
    }

    @Override
    @Transactional
    public void update(Long id, RegiaoRequestDTO dto) {
        Regiao regiao = findById(id);
        if (dto.version() != null && !regiao.getVersion().equals(dto.version())) {
            throw new OptimisticLockException(
                    "Conflito de concorrência: a região foi alterada por outra transação.");
        }
        regiao.setNome(dto.nome());
        repository.persist(regiao);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }
}

