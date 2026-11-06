package leepans.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import leepans.model.MetodoPagamento;

@Converter(autoApply = true)
public class MetodoPagamentoConverter implements AttributeConverter<MetodoPagamento, Long>{

    @Override
    public Long convertToDatabaseColumn(MetodoPagamento metodo) {
        return metodo==null ? null : metodo.getId();
    }

    @Override
    public MetodoPagamento convertToEntityAttribute(Long id) {
        return MetodoPagamento.valueOf(id);
    }
    
}
