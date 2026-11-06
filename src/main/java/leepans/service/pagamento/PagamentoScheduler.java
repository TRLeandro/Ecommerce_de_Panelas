package leepans.service.pagamento;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import leepans.service.ecommerce.PagamentoService;

@ApplicationScoped
public class PagamentoScheduler {

    private static final Logger LOG = Logger.getLogger(PagamentoScheduler.class);

    @Inject
    PagamentoService pagamentoService;

    @Scheduled(every = "${app.pagamento.expiracao.intervalo:5m}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void expirarPagamentos() {
        int processados = pagamentoService.expirarPagamentosVencidos();
        LOG.infof("Expiração de pagamentos: %d item(ns) processado(s).", processados);
    }
}
