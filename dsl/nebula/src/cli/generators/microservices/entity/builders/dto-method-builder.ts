import { EntityExt } from "../../../../types/ast-extensions.js";
import type { DtoSchemaRegistry } from "../../../../services/dto-schema-service.js";
import { ImportScanner } from "./import-scanner.js";



export class DtoMethodBuilder {
    constructor(
        private dtoRegistry: DtoSchemaRegistry | undefined,
        private importScanner: ImportScanner
    ) {}

    

    generateBuildDtoMethod(entity: EntityExt): string {
        const entityName = entity.name;

        
        const dtoTypeName = `${entityName}Dto`;
        const dtoSchema = this.dtoRegistry?.dtoByName?.[dtoTypeName];

        if (!dtoSchema) {
            
            return `\n    public ${dtoTypeName} buildDto() {\n        return new ${dtoTypeName}(this);\n    }`;
        }

        const dtoFieldOverrides = this.importScanner.resolveDtoFieldMappings(entity);
        const setterLines = dtoSchema.fields
            .map(field => this.importScanner.buildDtoSetterFromSchema(field, entity, dtoFieldOverrides))
            .filter((line): line is string => !!line);

        return `\n    public ${dtoTypeName} buildDto() {\n        ${dtoTypeName} dto = new ${dtoTypeName}();\n${setterLines.join('\n')}\n        return dto;\n    }`;
    }
}
