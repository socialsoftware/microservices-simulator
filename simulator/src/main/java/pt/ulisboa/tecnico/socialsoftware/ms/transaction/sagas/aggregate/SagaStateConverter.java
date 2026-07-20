package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SagaStateConverter implements AttributeConverter<SagaAggregate.SagaState, String> {

    @Override
    public String convertToDatabaseColumn(SagaAggregate.SagaState attribute) {
        if (attribute == null) {
            return null;
        }
        Enum<?> enumAttribute = (Enum<?>) attribute;
        // Store as FullyQualifiedClassName:ENUM_NAME
        return enumAttribute.getDeclaringClass().getName() + ":" + enumAttribute.name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SagaAggregate.SagaState convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            int colonIndex = dbData.indexOf(':');
            if (colonIndex == -1) {
                // Fallback for legacy data or unexpected format
                throw new IllegalArgumentException("Invalid format for SagaState: " + dbData);
            }
            String className = dbData.substring(0, colonIndex);
            String enumName = dbData.substring(colonIndex + 1);
            Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(className);
            return (SagaAggregate.SagaState) Enum.valueOf(enumClass, enumName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert string to SagaState: " + dbData, e);
        }
    }
}