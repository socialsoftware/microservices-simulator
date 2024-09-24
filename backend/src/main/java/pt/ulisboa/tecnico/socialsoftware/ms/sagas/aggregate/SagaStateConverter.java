package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SagaStateConverter implements AttributeConverter<SagaState, String> {

    @Override
    public String convertToDatabaseColumn(SagaState sagaState) {
        return sagaState != null ? sagaState.getStateName() : null;
    }

    // TODO: check this
    @Override
    public SagaState convertToEntityAttribute(String dbData) {
        // Map the dbData to the correct SagaState implementation
        return null;
    }
}