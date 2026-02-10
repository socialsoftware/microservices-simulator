import { EntityExt } from "../../../../types/ast-extensions.js";
import type { DtoSchemaRegistry } from "../../../../services/dto-schema-service.js";
import { ImportScanner } from "./import-scanner.js";

/**
 * Handles generation of the buildDto() method for entities.
 *
 * The buildDto() method converts an entity instance to its corresponding DTO.
 * This builder generates the method implementation using the DTO schema registry
 * to map entity properties to DTO fields.
 *
 * Responsibilities:
 * - Generate buildDto() method for all entities
 * - Use DTO schema to determine field mappings
 * - Delegate to ImportScanner for complex setter generation
 * - Provide fallback for entities without schema
 */
export class DtoMethodBuilder {
    constructor(
        private dtoRegistry: DtoSchemaRegistry | undefined,
        private importScanner: ImportScanner
    ) {}

    /**
     * Generates the buildDto() method for an entity
     */
    generateBuildDtoMethod(entity: EntityExt): string {
        const entityName = entity.name;

        // All entities now get their own DTOs, so all entities should have a buildDto() method
        const dtoTypeName = `${entityName}Dto`;
        const dtoSchema = this.dtoRegistry?.dtoByName?.[dtoTypeName];

        if (!dtoSchema) {
            // Fallback: generate simple constructor-based buildDto if no schema found
            return `\n    public ${dtoTypeName} buildDto() {\n        return new ${dtoTypeName}(this);\n    }`;
        }

        const dtoFieldOverrides = this.importScanner.resolveDtoFieldMappings(entity);
        const setterLines = dtoSchema.fields
            .map(field => this.importScanner.buildDtoSetterFromSchema(field, entity, dtoFieldOverrides))
            .filter((line): line is string => !!line);

        return `\n    public ${dtoTypeName} buildDto() {\n        ${dtoTypeName} dto = new ${dtoTypeName}();\n${setterLines.join('\n')}\n        return dto;\n    }`;
    }
}
