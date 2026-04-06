import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";

export interface WebApiGenerationOptions {
    architecture?: string;
    projectName: string;
    dtoSchemaRegistry?: DtoSchemaRegistry;
}
