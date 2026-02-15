import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";



export interface DtoSetterStrategy {
    

    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean;

    

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null;
}



export interface StrategyContext {
    buildEntityGetterCall(prop: any): string | null;
    isEnumProperty(prop: any): boolean;
}
