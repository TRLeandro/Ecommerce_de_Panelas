package leepans.service.ecommerce;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import leepans.dto.cidade.CidadeRequestDTO;
import leepans.exception.ResourceNotFoundException;
import leepans.model.Cidade;
import leepans.model.Estado;
import leepans.repository.CidadeRepository;
import leepans.repository.EstadoRepository;

@ApplicationScoped
public class CidadeService implements CidadeServiceInter {

    @Inject
    CidadeRepository repository;

    @Inject
    EstadoRepository estadoRepository;

    @Override
    public List<Cidade> findAll() {
        return repository.findAll().list();
    }

    @Override
    public Cidade findById(Long id) {
        Cidade cidade = repository.findById(id);
        if (cidade == null) {
            throw new ResourceNotFoundException("Cidade", id);
        }
        return cidade;
    }

    @Override
    public List<Cidade> findByNome(String nome) {
        return repository.findByNome(nome).list();
    }

    @Override
    public List<Cidade> findByEstadoId(Long idEstado) {
        return repository.findByEstadoId(idEstado).list();
    }

    @Override
    @Transactional
    public Cidade create(Cidade cidade) {
        if (cidade.getEstado() != null && cidade.getEstado().getId() != null) {
            Estado estado = estadoRepository.findById(cidade.getEstado().getId());
            if (estado == null) {
                throw new ResourceNotFoundException("Estado", cidade.getEstado().getId());
            }
            cidade.setEstado(estado);
        }
        repository.persist(cidade);
        return cidade;
    }

    @Override
    @Transactional
    public void update(Long id, CidadeRequestDTO dto) {
        Cidade cidade = findById(id);
        if (dto.version() != null && !cidade.getVersion().equals(dto.version())) {
            throw new OptimisticLockException(
                    "Conflito de concorrência: a cidade foi alterada por outra transação.");
        }
        cidade.setNome(dto.nome());
        if (dto.idEstado() != null) {
            Estado estado = estadoRepository.findById(dto.idEstado());
            if (estado == null) {
                throw new ResourceNotFoundException("Estado", dto.idEstado());
            }
            cidade.setEstado(estado);
        }
        repository.persist(cidade);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }
}

