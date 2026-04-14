import { EntityExt } from "../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../services/dto-schema-service.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";

export interface DtoSetterContext {
    buildEntityGetterCall(prop: any): string | null;
    isEnumProperty(prop: any): boolean;
}

export class DtoSetterBuilder {
    constructor(private context: DtoSetterContext) {}

    buildSetter(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const effectiveExtractField = override?.extractField || field.extractField;

        if (field.isAggregateField && !override && (entity.isRoot || false)) {
            return this.buildAggregateField(field);
        }

        if (override && !effectiveExtractField) {
            return this.buildOverride(capName, prop, getterCall);
        }

        if (effectiveExtractField) {
            return this.buildExtract(capName, field, prop, effectiveExtractField, getterCall);
        }

        if (field.derivedAggregateId && field.sourceProperty) {
            return this.buildDerivedAggregateId(capName, field, getterCall);
        }

        if (field.isEnum || (prop && this.context.isEnumProperty(prop))) {
            return this.buildEnum(capName, field, getterCall);
        }

        if (field.requiresConversion) {
            return this.buildDtoConversion(capName, field, getterCall);
        }

        if (prop?.name?.endsWith('Type')) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.toString() : null);`;
        }

        return `        dto.set${capName}(${getterCall});`;
    }

    private buildAggregateField(field: DtoFieldSchema): string | null {
        switch (field.name) {
            case 'aggregateId': return '        dto.setAggregateId(getAggregateId());';
            case 'version': return '        dto.setVersion(getVersion());';
            case 'state': return '        dto.setState(getState());';
            default: return null;
        }
    }

    private buildOverride(capName: string, prop: any, getterCall: string): string {
        if (this.context.isEnumProperty(prop)) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
        }
        return `        dto.set${capName}(${getterCall});`;
    }

    private buildExtract(capName: string, field: DtoFieldSchema, prop: any, extractField: string, getterCall: string): string | null {
        const extractMethod = `get${extractField.charAt(0).toUpperCase() + extractField.slice(1)}()`;
        const javaType = TypeResolver.resolveJavaType(prop.type);
        const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');

        if (isEntityCollection) {
            const elementTypeMatch = javaType.match(/<(.*)>/);
            if (elementTypeMatch) {
                const elType = elementTypeMatch[1];
                if (TypeResolver.isPrimitiveType(elType) || ['Integer', 'String', 'Boolean', 'Long'].includes(elType)) {
                    return null;
                }
            }
        }

        if (isEntityCollection && field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            const elementTypeMatch = javaType.match(/<(.*)>/);
            const elementType = elementTypeMatch ? elementTypeMatch[1] : 'item';
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
        }

        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
    }

    private buildDerivedAggregateId(capName: string, field: DtoFieldSchema, getterCall: string): string {
        const accessor = field.derivedAccessor || 'getAggregateId';
        if (field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null);`;
        }
        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${accessor}() : null);`;
    }

    private buildEnum(capName: string, field: DtoFieldSchema, getterCall: string): string {
        if (field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null);`;
        }
        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
    }

    private buildDtoConversion(capName: string, field: DtoFieldSchema, getterCall: string): string | null {
        if (field.isCollection && field.referencedEntityName) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedEntityName}::buildDto).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedDtoName}::new).collect(${collector}) : null);`;
        }
        if (!field.isCollection) {
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.buildDto() : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? new ${field.referencedDtoName}(${getterCall}) : null);`;
        }
        return null;
    }
}
