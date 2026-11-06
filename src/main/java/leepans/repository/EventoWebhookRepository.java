package leepans.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import leepans.model.EventoWebhook;

@ApplicationScoped
public class EventoWebhookRepository implements PanacheRepository<EventoWebhook> {

    public EventoWebhook findByGatewayEEvento(String gateway, String eventoId) {
        return find("gateway = ?1 AND eventoId = ?2", gateway, eventoId).firstResult();
    }

    public EventoWebhook findByGatewayEEventoParaAtualizacao(String gateway, String eventoId) {
        return find("gateway = ?1 AND eventoId = ?2", gateway, eventoId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
    }
}
