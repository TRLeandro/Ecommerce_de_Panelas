package leepans.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import leepans.model.TipoTransacao;

@Converter(autoApply = true)
public class TipoTransacaoConverter implements AttributeConverter<TipoTransacao, Long>{

    @Override
    public Long convertToDatabaseColumn(TipoTransacao tipo) {
        return tipo==null ? null : tipo.getId();
    }

    @Override
    public TipoTransacao convertToEntityAttribute(Long id) {
        return TipoTransacao.valueOf(id);
    }
    
}
