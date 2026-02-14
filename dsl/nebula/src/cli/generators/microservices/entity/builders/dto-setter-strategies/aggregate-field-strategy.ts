import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy } from "./dto-setter-strategy.js";

/**
 * Strategy for handling aggregate system fields (aggregateId, version, state).
 *
 * Only applies to root entities.
 */
export class AggregateFieldStrategy implements DtoSetterStrategy {
    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        const isRootEntity = entity.isRoot || false;
        return !!(field.isAggregateField && !override && isRootEntity);
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        switch (field.name) {
            case 'aggregateId':
                return '        dto.setAggregateId(getAggregateId());';
            case 'version':
                return '        dto.setVersion(getVersion());';
            case 'state':
                return '        dto.setState(getState());';
            default:
                return null;
        }
    }
}
