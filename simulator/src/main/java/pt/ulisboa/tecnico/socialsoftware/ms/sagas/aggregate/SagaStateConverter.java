package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SagaStateConverter implements AttributeConverter<SagaAggregate.SagaState, String> {

    @Override
    public String convertToDatabaseColumn(SagaAggregate.SagaState sagaState) {
        return sagaState != null ? sagaState.getStateName() : null;
    }

    // TODO: check this
    @Override
    public SagaAggregate.SagaState convertToEntityAttribute(String dbData) {
        // Map the dbData to the correct SagaState implementation
        return null;
    }
}