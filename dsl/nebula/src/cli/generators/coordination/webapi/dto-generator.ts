import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";

export interface CrossAggregateReference {
    entityType: string;      // The projection entity name (e.g., "ExecutionUser")
    paramName: string;       // The property name (e.g., "users")
    relatedAggregate: string; // The referenced aggregate name (e.g., "User")
    relatedDtoType: string;  // The DTO type to use (e.g., "UserDto")
    isCollection: boolean;   // Whether it's a collection
}

export class WebApiDtoGenerator extends WebApiBaseGenerator {
    /**
     * Generate request DTOs as a map of className -> content
     */
    async generateRequestDtos(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): Promise<Record<string, string>> {
        const context = this.buildRequestDtosContext(aggregate, rootEntity, options, allAggregates);
        const results: Record<string, string> = {};
        
        // Generate each DTO as a separate file
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
        
        // Add imports
        for (const imp of imports) {
            content += `${imp}\n`;
        }
        if (imports.length > 0) {
            content += '\n';
        }
        
        content += `public class ${dto.name} {\n`;
        
        // Add fields
        for (const field of dto.fields) {
            if (field.required) {
                content += `    @NotNull\n`;
            }
            content += `    private ${field.type} ${field.name};\n`;
        }
        content += '\n';
        
        // No-args constructor
        content += `    public ${dto.name}() {}\n\n`;

        // All-args constructor
        if (dto.fields.length > 0) {
            const params = dto.fields.map((field: any) => `${field.type} ${field.name}`).join(', ');
            content += `    public ${dto.name}(${params}) {\n`;
            for (const field of dto.fields) {
                content += `        this.${field.name} = ${field.name};\n`;
            }
            content += `    }\n\n`;
        }
        
        // Getters and setters
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
        return this.renderTemplate(template, context);
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
            packageName: `${getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi', 'requestDtos')}`,
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

        // Find cross-aggregate references for create DTO
        const crossAggregateRefs = this.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

        dtos.push({
            name: `Create${aggregateName}RequestDto`,
            fields: this.extractCreateDtoFields(rootEntity, aggregate, dtoRegistry, crossAggregateRefs),
            crossAggregateRefs // Store for import generation
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

    /**
     * Find cross-aggregate references from the root entity's properties.
     * These are non-root entities that have "uses AnotherAggregate" declarations.
     */
    findCrossAggregateReferences(rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): CrossAggregateReference[] {
        const references: CrossAggregateReference[] = [];

        if (!rootEntity.properties) {
            return references;
        }

        const entities = getEntities(aggregate);

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Resolve entity type
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Find the entity in this aggregate
                const relatedEntity = entities.find(e => e.name === entityName);
                if (!relatedEntity) continue;

                // Check if the entity has aggregateRef (uses another aggregate)
                const entityAny = relatedEntity as any;
                const aggregateRef = entityAny.aggregateRef;

                if (aggregateRef) {
                    // Get the referenced aggregate name
                    let relatedAggregateName: string | undefined;
                    if (typeof aggregateRef === 'string') {
                        relatedAggregateName = aggregateRef;
                    } else if (aggregateRef.ref?.name) {
                        relatedAggregateName = aggregateRef.ref.name;
                    } else if (aggregateRef.$refText) {
                        relatedAggregateName = aggregateRef.$refText;
                    }

                    if (relatedAggregateName) {
                        // Verify this is a different aggregate
                        const isFromAnotherAggregate = allAggregates?.some(
                            agg => agg.name === relatedAggregateName && agg.name !== aggregate.name
                        );

                        if (isFromAnotherAggregate) {
                            references.push({
                                entityType: entityName,
                                paramName: prop.name,
                                relatedAggregate: relatedAggregateName,
                                relatedDtoType: `${relatedAggregateName}Dto`,
                                isCollection
                            });
                        }
                    }
                }
            }
        }

        return references;
    }

    /**
     * Extract fields for the Create request DTO.
     * For cross-aggregate references, include the full DTO type instead of aggregate IDs.
     * For same-aggregate entity references, skip them (they'll be created internally).
     * For primitive fields, include them directly.
     */
    private extractCreateDtoFields(entity: Entity, aggregate: Aggregate, dtoRegistry?: DtoSchemaRegistry, crossAggregateRefs?: CrossAggregateReference[]): any[] {
        const fields: any[] = [];
        const crossRefParamNames = new Set(crossAggregateRefs?.map(r => r.paramName) || []);
        const entities = getEntities(aggregate);

        // Add cross-aggregate DTO references
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

        // Add primitive/non-entity fields from root entity
        if (entity.properties) {
            for (const prop of entity.properties) {
                if ((prop as any).dtoExclude) continue;
                if (prop.name.toLowerCase() === 'id') continue;

                // Skip cross-aggregate references (already handled above)
                if (crossRefParamNames.has(prop.name)) continue;

                const javaType = this.resolveParameterType(prop.type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

                // Check if this is an entity type within the same aggregate
                const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
                
                if (isEntityType) {
                    // For same-aggregate entities, skip them - they'll be created internally
                    // or include their own DTO if needed
                    const entityRef = (prop.type as any).type?.ref;
                    let entityName: string;

                    if (isCollection) {
                        const elementType = TypeResolver.getElementType(prop.type);
                        entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                    } else {
                        entityName = entityRef?.name || javaType;
                    }

                    // Check if this is an entity in the same aggregate without cross-aggregate ref
                    const relatedEntity = entities.find(e => e.name === entityName);
                    if (relatedEntity) {
                        // Same-aggregate entity - skip it (will be created internally or use projection DTO)
                        continue;
                    }
                }

                // Include primitive/enum fields
                fields.push({
                    name: prop.name,
                    type: javaType,
                    required: true
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

        // Add imports for cross-aggregate DTOs
        for (const dto of requestDtos) {
            const crossAggregateRefs = dto.crossAggregateRefs as CrossAggregateReference[] | undefined;
            if (crossAggregateRefs) {
                for (const ref of crossAggregateRefs) {
                    const dtoImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos') + '.' + ref.relatedDtoType;
                    imports.add(`import ${dtoImportPath};`);
                }
            }

            // Check each field for imports needed
            for (const field of dto.fields) {
                const fieldType = field.type as string;
                if (!fieldType) continue;

                // Check for collection types
                if (fieldType.includes('List<')) {
                    imports.add('import java.util.List;');
                }
                if (fieldType.includes('Set<')) {
                    imports.add('import java.util.Set;');
                }

                // Check for LocalDateTime type
                if (fieldType === 'LocalDateTime' || fieldType.includes('LocalDateTime')) {
                    imports.add('import java.time.LocalDateTime;');
                }

                // Check for enum types (types ending with "Type", "State", or "Role" but not AggregateState)
                if (fieldType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/) && fieldType !== 'AggregateState') {
                    const enumImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'enums') + '.' + fieldType;
                    imports.add(`import ${enumImportPath};`);
                }

                // Check for DTO types (types ending with "Dto")
                if (fieldType.match(/^[A-Z][a-zA-Z]*Dto$/)) {
                    const dtoImportPath = getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos') + '.' + fieldType;
                    imports.add(`import ${dtoImportPath};`);
                }

                // Check for DTO types inside collections (Set<XxxDto> or List<XxxDto>)
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

    /**
     * Check if a type is an enum
     */
    private isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                return true;
            }
            const ref = type.type.ref;
            if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }

    private getResponseDtosTemplate(): string {
        return this.loadTemplate('web/response-dtos.hbs');
    }
}
