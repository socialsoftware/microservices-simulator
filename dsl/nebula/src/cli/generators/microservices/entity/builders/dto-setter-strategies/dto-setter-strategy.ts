import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";

/**
 * Strategy interface for building DTO setter statements.
 *
 * Each strategy handles a specific case of DTO field mapping.
 */
export interface DtoSetterStrategy {
    /**
     * Check if this strategy can handle the given field.
     */
    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean;

    /**
     * Build the DTO setter statement for this field.
     */
    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null;
}

/**
 * Context object containing shared utilities for strategies.
 */
export interface StrategyContext {
    buildEntityGetterCall(prop: any): string | null;
    isEnumProperty(prop: any): boolean;
}
