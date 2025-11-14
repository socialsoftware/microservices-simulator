import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";

export class WebApiDtoGenerator extends WebApiBaseGenerator {
    async generateRequestDtos(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildRequestDtosContext(aggregate, rootEntity, options);
        const template = this.getRequestDtosTemplate();
        return this.renderTemplate(template, context);
    }

    async generateResponseDtos(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildResponseDtosContext(aggregate, rootEntity, options);
        const template = this.getResponseDtosTemplate();
        return this.renderTemplate(template, context);
    }

    private buildRequestDtosContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const requestDtos = this.buildRequestDtos(rootEntity, capitalizedAggregate, options.dtoSchemaRegistry);
        const imports = this.buildRequestDtosImports(aggregate, options, requestDtos);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi', 'dtos')}`,
            requestDtos,
            imports
        };
    }

    private buildResponseDtosContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const responseDtos = this.buildResponseDtos(rootEntity, capitalizedAggregate, options.dtoSchemaRegistry);
        const imports = this.buildResponseDtosImports(aggregate, options, responseDtos);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi', 'dtos')}`,
            responseDtos,
            imports
        };
    }

    private buildRequestDtos(rootEntity: Entity, aggregateName: string, dtoRegistry?: DtoSchemaRegistry): any[] {
        const dtos: any[] = [];

        dtos.push({
            name: `Create${aggregateName}RequestDto`,
            fields: this.extractDtoFields(rootEntity, 'create', dtoRegistry)
        });

        dtos.push({
            name: `Update${aggregateName}RequestDto`,
            fields: this.extractDtoFields(rootEntity, 'update', dtoRegistry)
        });

        return dtos;
    }

    private buildResponseDtos(rootEntity: Entity, aggregateName: string, dtoRegistry?: DtoSchemaRegistry): any[] {
        const dtos: any[] = [];

        dtos.push({
            name: `${aggregateName}ResponseDto`,
            fields: this.extractDtoFields(rootEntity, 'response', dtoRegistry)
        });

        return dtos;
    }

    private extractDtoFields(entity: Entity, type: 'create' | 'update' | 'response', dtoRegistry?: DtoSchemaRegistry): any[] {
        const dtoSchema = dtoRegistry?.entityToDto?.[entity.name];

        if (dtoSchema) {
            return dtoSchema.fields
                .filter(field => this.includeFieldInRequestDto(field, type))
                .map(field => ({
                    name: field.name,
                    type: field.javaType,
                    required: type !== 'response'
                }));
        }

        const fields: any[] = [];

        if (entity.properties) {
            entity.properties.forEach(prop => {
                if ((prop as any).dtoExclude) {
                    return;
                }

                if (type === 'create' && prop.name.toLowerCase() === 'id') {
                    return;
                }

                fields.push({
                    name: prop.name,
                    type: this.resolveParameterType(prop.type),
                    required: true
                });
            });
        }

        return fields;
    }

    private includeFieldInRequestDto(field: DtoFieldSchema, type: 'create' | 'update' | 'response'): boolean {
        if (field.isAggregateField) {
            return type === 'response';
        }

        if (type === 'create' && field.sourceName?.toLowerCase() === 'id') {
            return false;
        }

        return true;
    }

    private buildRequestDtosImports(aggregate: Aggregate, options: WebApiGenerationOptions, requestDtos: any[]): string[] {
        const imports = new Set<string>();

        const hasValidation = requestDtos.some(dto =>
            dto.fields.some((field: any) => field.required)
        );

        if (hasValidation) {
            imports.add('import jakarta.validation.constraints.*;');
        }

        return Array.from(imports);
    }

    private buildResponseDtosImports(aggregate: Aggregate, options: WebApiGenerationOptions, responseDtos: any[]): string[] {
        const imports = new Set<string>();

        imports.add('import java.time.LocalDateTime;');

        return Array.from(imports);
    }

    private getRequestDtosTemplate(): string {
        return this.loadTemplate('web/request-dtos.hbs');
    }

    private getResponseDtosTemplate(): string {
        return this.loadTemplate('web/response-dtos.hbs');
    }
}
