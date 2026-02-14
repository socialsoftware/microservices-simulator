import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy, StrategyContext } from "./dto-setter-strategy.js";

/**
 * Strategy for handling field mapping overrides.
 *
 * Overrides are explicitly specified mappings that take precedence over default behavior.
 */
export class OverrideFieldStrategy implements DtoSetterStrategy {
    constructor(private context: StrategyContext) {}

    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        return !!override && !override.extractField && !field.extractField;
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);

        // Handle enum override
        if (this.context.isEnumProperty(prop)) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
        }

        // Simple override
        return `        dto.set${capName}(${getterCall});`;
    }
}
