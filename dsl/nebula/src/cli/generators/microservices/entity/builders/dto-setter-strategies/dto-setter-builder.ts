import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy, StrategyContext } from "./dto-setter-strategy.js";
import { AggregateFieldStrategy } from "./aggregate-field-strategy.js";
import { OverrideFieldStrategy } from "./override-field-strategy.js";
import { ExtractFieldStrategy } from "./extract-field-strategy.js";
import { DerivedAggregateIdStrategy } from "./derived-aggregate-id-strategy.js";
import { EnumFieldStrategy } from "./enum-field-strategy.js";
import { DtoConversionStrategy } from "./dto-conversion-strategy.js";
import { LegacyTypeStrategy } from "./legacy-type-strategy.js";
import { DefaultSetterStrategy } from "./default-setter-strategy.js";

/**
 * Builder for DTO setter statements using the Strategy pattern.
 *
 * Replaces the complex buildDtoSetterFromSchema method with a clean,
 * extensible architecture where each case is handled by a dedicated strategy.
 */
export class DtoSetterBuilder {
    private strategies: DtoSetterStrategy[];

    constructor(context: StrategyContext) {
        // Order matters! More specific strategies should come before general ones.
        this.strategies = [
            new AggregateFieldStrategy(),
            new OverrideFieldStrategy(context),
            new ExtractFieldStrategy(),
            new DerivedAggregateIdStrategy(),
            new EnumFieldStrategy(context),
            new DtoConversionStrategy(),
            new LegacyTypeStrategy(),
            new DefaultSetterStrategy(), // Fallback - always last
        ];
    }

    /**
     * Build a DTO setter statement for the given field.
     *
     * Iterates through strategies until one can handle the field.
     */
    buildSetter(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        for (const strategy of this.strategies) {
            if (strategy.canHandle(field, entity, prop, override)) {
                return strategy.build(field, entity, prop, override, getterCall);
            }
        }

        // Should never reach here because DefaultSetterStrategy always returns true
        return null;
    }
}
