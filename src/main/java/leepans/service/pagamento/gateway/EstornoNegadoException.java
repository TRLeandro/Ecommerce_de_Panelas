package leepans.service.pagamento.gateway;

import leepans.exception.BusinessRuleViolationException;

/** O gateway respondeu e não aceitou o estorno. Tipo próprio para não desfazer o registro da tentativa. */
public class EstornoNegadoException extends BusinessRuleViolationException {

    private static final long serialVersionUID = 1L;

    public EstornoNegadoException(String message) {
        super(message);
    }
}
