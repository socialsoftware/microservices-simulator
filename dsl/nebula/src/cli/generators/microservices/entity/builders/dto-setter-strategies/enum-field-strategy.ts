import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy, StrategyContext } from "./dto-setter-strategy.js";

/**
 * Strategy for handling enum fields.
 *
 * Converts enums to strings using .name() method.
 */
export class EnumFieldStrategy implements DtoSetterStrategy {
    constructor(private context: StrategyContext) {}

    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        // Don't handle if there's an extract field (that takes precedence)
        const effectiveExtractField = override?.extractField || field.extractField;
        if (effectiveExtractField) {
            return false;
        }

        return field.isEnum || (prop && this.context.isEnumProperty(prop));
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);

        if (field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null);`;
        }

        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
    }
}
