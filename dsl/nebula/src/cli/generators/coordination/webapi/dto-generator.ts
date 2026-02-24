import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";

export interface CrossAggregateReference {
    entityType: string;
    paramName: string;
    relatedAggregate: string;
    relatedDtoType: string;
    isCollection: boolean;
}

export class WebApiDtoGenerator extends WebApiBaseGenerator {


    async generateRequestDtos(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): Promise<Record<string, string>> {
        const context = this.buildRequestDtosContext(aggregate, rootEntity, options, allAggregates);
        const results: Record<string, string> = {};


        for (const dto of context.requestDtos) {
            const dtoContext = {
                packageName: context.packageName,
                imports: context.imports,
                dto
            };
            const content = this.renderSingleRequestDto(dtoContext);
            results[dto.name] = content;
        }

        return results;
    }

    private renderSingleRequestDto(context: { packageName: string; imports: string[]; dto: any }): string {
        const { packageName, imports, dto } = context;

        let content = `package ${packageName};\n\n`;


        for (const imp of imports) {
            content += `${imp}\n`;
        }
        if (imports.length > 0) {
            content += '\n';
        }

        content += `public class ${dto.name} {\n`;


        for (const field of dto.fields) {
            if (field.required) {
                content += `    @NotNull\n`;
            }
            content += `    private ${field.type} ${field.name};\n`;
        }
        content += '\n';


        content += `    public ${dto.name}() {}\n\n`;


        if (dto.fields.length > 0) {
            const params = dto.fields.map((field: any) => `${field.type} ${field.name}`).join(', ');
            content += `    public ${dto.name}(${params}) {\n`;
            for (const field of dto.fields) {
                content += `        this.${field.name} = ${field.name};\n`;
            }
            content += `    }\n\n`;
        }


        for (const field of dto.fields) {
            const capitalizedName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
            content += `    public ${field.type} get${capitalizedName}() {\n`;
            content += `        return ${field.name};\n`;
            content += `    }\n\n`;

            content += `    public void set${capitalizedName}(${field.type} ${field.name}) {\n`;
            content += `        this.${field.name} = ${field.name};\n`;
            content += `    }\n`;
        }

        content += `}\n`;

        return content;
    }

    async generateResponseDtos(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildResponseDtosContext(aggregate, rootEntity, options);
        const template = this.getResponseDtosTemplate();
        return this.renderTemplateFromString(template, context);
    }

    private buildRequestDtosContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const requestDtos = this.buildRequestDtos(aggregate, rootEntity, capitalizedAggregate, options.dtoSchemaRegistry, allAggregates);
        const imports = this.buildRequestDtosImports(aggregate, options, requestDtos);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${getGlobalConfig().buildPackageName(options.projectName, 'microservices', lowerAggregate, 'coordination', 'webapi', 'requestDtos')}`,
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

    private buildRequestDtos(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, dtoRegistry?: DtoSchemaRegistry, allAggregates?: Aggregate[]): any[] {
        const dtos: any[] = [];


        const crossAggregateRefs = this.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

        dtos.push({
            name: `Create${aggregateName}RequestDto`,
            fields: this.extractCreateDtoFields(rootEntity, aggregate, dtoRegistry, crossAggregateRefs),
            crossAggregateRefs
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



    findCrossAggregateReferences(rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): CrossAggregateReference[] {
        const references: CrossAggregateReference[] = [];

        if (!rootEntity.properties) {
            return references;
        }

        const entities = getEntities(aggregate);

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');


            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {

                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }


                const relatedEntity = entities.find(e => e.name === entityName);
                if (!relatedEntity) continue;


                const entityAny = relatedEntity as any;
                const aggregateRef = entityAny.aggregateRef;

                if (aggregateRef) {

                    let referencedName: string | undefined;
                    if (typeof aggregateRef === 'string') {
                        referencedName = aggregateRef;
                    } else if (aggregateRef.ref?.name) {
                        referencedName = aggregateRef.ref.name;
                    } else if (aggregateRef.$refText) {
                        referencedName = aggregateRef.$refText;
                    }

                    if (referencedName) {

                        const directAggregate = allAggregates?.find(
                            agg => agg.name === referencedName && agg.name !== aggregate.name
                        );

                        if (directAggregate) {

                            references.push({
                                entityType: entityName,
                                paramName: prop.name,
                                relatedAggregate: referencedName,
                                relatedDtoType: `${referencedName}Dto`,
                                isCollection
                            });
                        } else {


                            const ultimateAggregate = this.resolveTransitiveAggregateRef(referencedName, allAggregates);
                            if (ultimateAggregate) {
                                references.push({
                                    entityType: entityName,
                                    paramName: prop.name,
                                    relatedAggregate: ultimateAggregate,
                                    relatedDtoType: `${ultimateAggregate}Dto`,
                                    isCollection
                                });
                            }
                        }
                    }
                }
            }
        }

        return references;
    }



    private resolveTransitiveAggregateRef(entityName: string, allAggregates?: Aggregate[]): string | undefined {
        if (!allAggregates) return undefined;


        for (const agg of allAggregates) {
            const entities = getEntities(agg);
            const entity = entities.find(e => e.name === entityName);

            if (entity) {
                const entityAny = entity as any;
                const aggregateRef = entityAny.aggregateRef;

                if (aggregateRef) {
                    let refName: string | undefined;
                    if (typeof aggregateRef === 'string') {
                        refName = aggregateRef;
                    } else if (aggregateRef.ref?.name) {
                        refName = aggregateRef.ref.name;
                    } else if (aggregateRef.$refText) {
                        refName = aggregateRef.$refText;
                    }

                    if (refName) {

                        const directAgg = allAggregates.find(a => a.name === refName);
                        if (directAgg) {
                            return refName;
                        }

                        return this.resolveTransitiveAggregateRef(refName, allAggregates);
                    }
                }



                if (entityAny.isRoot) {
                    return agg.name;
                }
            }
        }

        return undefined;
    }



    private extractCreateDtoFields(entity: Entity, aggregate: Aggregate, dtoRegistry?: DtoSchemaRegistry, crossAggregateRefs?: CrossAggregateReference[]): any[] {
        const fields: any[] = [];
        const crossRefParamNames = new Set(crossAggregateRefs?.map(r => r.paramName) || []);
        const entities = getEntities(aggregate);


        for (const ref of crossAggregateRefs || []) {
            if (ref.isCollection) {
                const collectionType = ref.paramName.endsWith('s') ? 'Set' : 'List';
                fields.push({
                    name: ref.paramName,
                    type: `${collectionType}<${ref.relatedDtoType}>`,
                    required: true,
                    isCrossAggregateDto: true,
                    relatedDtoType: ref.relatedDtoType
                });
            } else {
                fields.push({
                    name: ref.paramName,
                    type: ref.relatedDtoType,
                    required: true,
                    isCrossAggregateDto: true,
                    relatedDtoType: ref.relatedDtoType
                });
            }
        }


        if (entity.properties) {
            for (const prop of entity.properties) {
                if ((prop as any).dtoExclude) continue;
                if (prop.name.toLowerCase() === 'id') continue;


                if (crossRefParamNames.has(prop.name)) continue;

                const javaType = this.resolveParameterType(prop.type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');


                const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

                if (isEntityType) {

                    const entityRef = (prop.type as any).type?.ref;
                    let entityName: string;

                    if (isCollection) {
                        const elementType = TypeResolver.getElementType(prop.type);
                        entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                    } else {
                        entityName = entityRef?.name || javaType;
                    }


                    const relatedEntity = entities.find(e => e.name === entityName);
                    if (relatedEntity) {
                        const entityAny = relatedEntity as any;

                        if (entityAny.generateDto && isCollection) {
                            const collectionPrefix = javaType.startsWith('Set<') ? 'Set' : 'List';
                            fields.push({
                                name: prop.name,
                                type: `${collectionPrefix}<${entityName}Dto>`,
                                required: false,
                                isProjectionDtoCollection: true
                            });
                        }

                        continue;
                    }
                }

                const isEnum = this.isEnumType(prop.type);

                fields.push({
                    name: prop.name,
                    type: javaType,
                    required: true,
                    isEnum
                });
            }
        }

        return fields;
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


        for (const dto of requestDtos) {
            const crossAggregateRefs = dto.crossAggregateRefs as CrossAggregateReference[] | undefined;
            if (crossAggregateRefs) {
                for (const ref of crossAggregateRefs) {
                    const dtoImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos') + '.' + ref.relatedDtoType;
                    imports.add(`import ${dtoImportPath};`);
                }
            }


            for (const field of dto.fields) {
                const fieldType = field.type as string;
                if (!fieldType) continue;


                if (fieldType.includes('List<')) {
                    imports.add('import java.util.List;');
                }
                if (fieldType.includes('Set<')) {
                    imports.add('import java.util.Set;');
                }


                if (fieldType === 'LocalDateTime' || fieldType.includes('LocalDateTime')) {
                    imports.add('import java.time.LocalDateTime;');
                }

                const isEnumField = field.isEnum || (fieldType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/) && fieldType !== 'AggregateState');
                if (isEnumField && fieldType !== 'AggregateState') {
                    const enumImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'enums') + '.' + fieldType;
                    imports.add(`import ${enumImportPath};`);
                }


                if (fieldType.match(/^[A-Z][a-zA-Z]*Dto$/)) {
                    const dtoImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos') + '.' + fieldType;
                    imports.add(`import ${dtoImportPath};`);
                }


                const collectionDtoMatch = fieldType.match(/(?:Set|List)<([A-Z][a-zA-Z]*Dto)>/);
                if (collectionDtoMatch) {
                    const dtoType = collectionDtoMatch[1];
                    const dtoImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos') + '.' + dtoType;
                    imports.add(`import ${dtoImportPath};`);
                }
            }
        }

        return Array.from(imports);
    }

    private buildResponseDtosImports(aggregate: Aggregate, options: WebApiGenerationOptions, responseDtos: any[]): string[] {
        const imports = new Set<string>();

        imports.add('import java.time.LocalDateTime;');

        return Array.from(imports);
    }



    private isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            const ref = type.type.ref;
            if (ref && typeof ref === 'object' && '$type' in ref && ((ref as any).$type === 'EnumDefinition' || (ref as any).$type === 'Enum')) {
                return true;
            }
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*(Type|State|Role|Status|Method|Kind|Mode|Level|Priority)$/)) {
                return true;
            }
        }
        return false;
    }

    private getResponseDtosTemplate(): string {
        return this.loadRawTemplate('web/response-dtos.hbs');
    }
}
