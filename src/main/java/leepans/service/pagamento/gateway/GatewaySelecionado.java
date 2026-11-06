package leepans.service.pagamento.gateway;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Marca o gateway escolhido em app.pagamento.gateway. O qualifier evita a
 * ambiguidade entre o producer e as próprias implementações na injeção.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface GatewaySelecionado {
}
