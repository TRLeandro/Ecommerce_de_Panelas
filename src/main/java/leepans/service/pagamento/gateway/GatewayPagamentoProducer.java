package leepans.service.pagamento.gateway;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayPagamentoProducer {

    @ConfigProperty(name = "app.pagamento.gateway")
    String gatewayConfigurado;

    @Inject
    Instance<GatewayPagamento> implementacoes;

    @Produces
    @ApplicationScoped
    @GatewaySelecionado
    GatewayPagamento gatewaySelecionado() {
        return selecionar(gatewayConfigurado, implementacoes);
    }

    /** O bean produzido é lazy: valida aqui para um valor errado derrubar a aplicação ao subir. */
    void validarConfiguracao(@Observes StartupEvent evento) {
        selecionar(gatewayConfigurado, implementacoes);
    }

    static GatewayPagamento selecionar(String nome, Iterable<GatewayPagamento> implementacoes) {
        List<String> disponiveis = new ArrayList<>();
        for (GatewayPagamento gateway : implementacoes) {
            if (gateway.nome().equals(nome)) {
                return gateway;
            }
            disponiveis.add(gateway.nome());
        }
        throw new IllegalStateException(
                "Gateway de pagamento desconhecido em app.pagamento.gateway: '" + nome
                        + "'. Disponíveis: " + disponiveis);
    }
}
