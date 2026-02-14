import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy } from "./dto-setter-strategy.js";

/**
 * Strategy for handling legacy Type fields.
 *
 * Converts fields ending with "Type" to strings using toString().
 * This is legacy behavior for backward compatibility.
 */
export class LegacyTypeStrategy implements DtoSetterStrategy {
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

        return prop?.name?.endsWith('Type') || false;
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.toString() : null);`;
    }
}
