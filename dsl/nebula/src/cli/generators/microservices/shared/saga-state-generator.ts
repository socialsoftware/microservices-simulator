import { getGlobalConfig } from "../../common/config.js";

export interface SagaStateDefinition {
    name: string;
    values: string[];
}

export interface SagaStateGenerationOptions {
    projectName: string;
    outputPath: string;
}

export class SagaStateGenerator {

    generateSagaStateCode(def: SagaStateDefinition, options: SagaStateGenerationOptions): string {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'sagaStates');
        const basePackage = config.getBasePackage();

        const enumValues = def.values.map(value => `    ${value} {
        @Override
        public String getStateName() {
            return "${value}";
        }
    }`).join(',\n');

        return `package ${packageName};

import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ${def.name} implements SagaState {
${enumValues}
}
`;
    }

    generateAll(defs: SagaStateDefinition[], options: SagaStateGenerationOptions): { [key: string]: string } {
        const results: { [key: string]: string } = {};
        for (const def of defs) {
            results[`shared/sagaStates/${def.name}.java`] = this.generateSagaStateCode(def, options);
        }
        results[`shared/sagaStates/SagaStateConverter.java`] = this.generateConverter(options);
        return results;
    }

    private generateConverter(options: SagaStateGenerationOptions): string {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'sagaStates');
        const basePackage = config.getBasePackage();
        return `package ${packageName};

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;

@Converter(autoApply = false)
public class SagaStateConverter implements AttributeConverter<SagaState, String> {

    @Override
    public String convertToDatabaseColumn(SagaState state) {
        if (state == null) return null;
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
`;
    }
}
