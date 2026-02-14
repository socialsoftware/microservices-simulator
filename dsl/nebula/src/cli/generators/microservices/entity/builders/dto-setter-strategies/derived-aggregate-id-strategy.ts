import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy } from "./dto-setter-strategy.js";

/**
 * Strategy for handling derived aggregate ID fields.
 *
 * Example: Extract aggregateId from a referenced entity or collection.
 */
export class DerivedAggregateIdStrategy implements DtoSetterStrategy {
    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        return !!field.derivedAggregateId && !!field.sourceProperty;
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const accessor = field.derivedAccessor || 'getAggregateId';

        if (field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null);`;
        }

        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${accessor}() : null);`;
    }
}
