package pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.sagaStates;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

@Converter(autoApply = false)
public class SagaStateConverter implements AttributeConverter<SagaState, String> {

    @Override
    public String convertToDatabaseColumn(SagaState state) {
        if (state == null) return null;
        // Use getDeclaringClass(): for enum values with bodies (like NOT_IN_SAGA { ... })
        // getClass() returns an anonymous subclass whose name can't be Enum.valueOf'd.
        Class<?> enumClass = ((Enum<?>) state).getDeclaringClass();
        return enumClass.getName() + "#" + ((Enum<?>) state).name();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SagaState convertToEntityAttribute(String value) {
        if (value == null) return null;
        int idx = value.indexOf('#');
        if (idx < 0) return null;
        try {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(value.substring(0, idx));
            return (SagaState) Enum.valueOf(enumClass, value.substring(idx + 1));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve saga-state class for value: " + value, e);
        }
    }
}
