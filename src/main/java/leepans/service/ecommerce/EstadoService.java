package leepans.service.ecommerce;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import leepans.dto.estado.EstadoRequestDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.model.Estado;
import leepans.model.Regiao;
import leepans.repository.EstadoRepository;
import leepans.repository.RegiaoRepository;

@ApplicationScoped
public class EstadoService implements EstadoServiceInter {

    @Inject
    EstadoRepository repository;

    @Inject
    RegiaoRepository regiaoRepository;

    @Override
    public List<Estado> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Estado findById(Long id) {
        Estado estado = repository.findById(id);
        if (estado == null) {
            throw new ResourceNotFoundException("Estado", id);
        }
        return estado;
    }

    @Override
    public List<Estado> findByNome(String nome) {
        return repository.findByNome(nome).list();
    }

    @Override
    public Estado findBySigla(String sigla) {
        Estado estado = repository.findBySigla(sigla).firstResult();
        if (estado == null) {
            throw new ResourceNotFoundException("Estado com sigla " + sigla + " não encontrado.");
        }
        return estado;
    }

    @Override
    public List<Estado> findByRegiaoId(Long idRegiao) {
        return repository.findByRegiaoId(idRegiao).list();
    }

    @Override
    @Transactional
    public Estado create(Estado estado) {
        if (estado.getRegiao() != null && estado.getRegiao().getId() != null) {
            Regiao regiao = regiaoRepository.findById(estado.getRegiao().getId());
            if (regiao == null) {
                throw new ResourceNotFoundException("Região", estado.getRegiao().getId());
            }
            estado.setRegiao(regiao);
        }
        repository.persist(estado);
        return estado;
    }

    @Override
    @Transactional
    public void update(Long id, EstadoRequestDTO dto) {
        Estado estado = findById(id);
        if (dto.version() != null && !estado.getVersion().equals(dto.version())) {
            throw new OptimisticLockException(
                    "Conflito de concorrência: o estado foi alterado por outra transação.");
        }
        estado.setNome(dto.nome());
        estado.setSigla(dto.sigla());
        if (dto.idRegiao() != null) {
            Regiao regiao = regiaoRepository.findById(dto.idRegiao());
            if (regiao == null) {
                throw new ResourceNotFoundException("Região", dto.idRegiao());
            }
            estado.setRegiao(regiao);
        }
        repository.persist(estado);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }
}

