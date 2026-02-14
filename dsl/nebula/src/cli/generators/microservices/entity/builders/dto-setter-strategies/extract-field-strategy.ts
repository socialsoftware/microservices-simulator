import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy } from "./dto-setter-strategy.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../../common/unified-type-resolver.js";

/**
 * Strategy for handling field extraction from nested objects.
 *
 * Examples:
 * - Extract aggregateId from collection: users.stream().map(item -> item.getAggregateId())
 * - Extract property from single object: course.getName()
 */
export class ExtractFieldStrategy implements DtoSetterStrategy {
    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        const effectiveExtractField = override?.extractField || field.extractField;
        return !!effectiveExtractField;
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const effectiveExtractField = override?.extractField || field.extractField;

        if (!effectiveExtractField) {
            return null;
        }

        const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
        const javaType = TypeResolver.resolveJavaType(prop.type);
        const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
        const isDtoCollection = field.isCollection;

        // Skip primitive element collections
        if (isEntityCollection) {
            const elementTypeMatch = javaType.match(/<(.*)>/);
            if (elementTypeMatch) {
                const elementType = elementTypeMatch[1];
                if (TypeResolver.isPrimitiveType(elementType) ||
                    elementType === 'Integer' ||
                    elementType === 'String' ||
                    elementType === 'Boolean' ||
                    elementType === 'Long') {
                    return null;
                }
            }
        }

        // Handle collection extraction
        if (isEntityCollection && isDtoCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            let elementType = 'item';
            if (javaType) {
                const elementTypeMatch = javaType.match(/<(.*)>/);
                if (elementTypeMatch) {
                    elementType = elementTypeMatch[1];
                }
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
        }

        // Handle single object extraction
        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
    }
}
