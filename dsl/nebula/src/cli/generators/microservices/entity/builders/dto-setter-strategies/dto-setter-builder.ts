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



export class DtoSetterBuilder {
    private strategies: DtoSetterStrategy[];

    constructor(context: StrategyContext) {
        
        this.strategies = [
            new AggregateFieldStrategy(),
            new OverrideFieldStrategy(context),
            new ExtractFieldStrategy(),
            new DerivedAggregateIdStrategy(),
            new EnumFieldStrategy(context),
            new DtoConversionStrategy(),
            new LegacyTypeStrategy(),
            new DefaultSetterStrategy(), 
        ];
    }

    

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

        
        return null;
    }
}
