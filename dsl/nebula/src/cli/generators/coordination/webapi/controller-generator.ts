import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

export class ControllerGenerator extends WebApiBaseGenerator {
    async generateController(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): Promise<string> {
        const context = this.buildControllerContext(aggregate, rootEntity, options, allAggregates);
        const template = this.getControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private buildControllerContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions, allAggregates?: Aggregate[]): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const endpoints = this.buildEndpoints(rootEntity, capitalizedAggregate, aggregate, allAggregates);
        const imports = this.buildControllerImports(aggregate, options, endpoints);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            functionalitiesName: `${aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1)}Functionalities`,
            packageName: getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi'),
            endpoints,
            imports,
            projectName: options.projectName.toLowerCase(),
            ProjectName: this.capitalize(options.projectName),
            hasLocalDtos: options.architecture === 'external-dto-removal',
            hasExternalDtos: options.architecture === 'default'
        };
    }

    private buildEndpoints(rootEntity: Entity, aggregateName: string, aggregate: Aggregate, allAggregates?: Aggregate[]): any[] {
        const endpoints: any[] = [];
        const lowerAggregate = aggregateName.toLowerCase();

        if (aggregate.generateCrud) {
            const crudEndpoints = this.generateCrudEndpoints(aggregateName, lowerAggregate, rootEntity, aggregate, allAggregates);
            endpoints.push(...crudEndpoints);
        }

        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                const resolvedReturnType = endpoint.returnType ? this.resolveParameterType(endpoint.returnType) : null;
                endpoints.push({
                    method: this.resolveHttpMethod(endpoint.method?.method || endpoint.httpMethod?.method),
                    path: endpoint.path,
                    methodName: endpoint.methodName,
                    parameters: endpoint.parameters.map((param: any) => ({
                        name: param.name,
                        type: this.resolveParameterType(param.type),
                        annotation: param.annotation
                    })),
                    returnType: resolvedReturnType,
                    description: endpoint.description || endpoint.desc,
                    throwsException: endpoint.throwsException === 'true'
                });
            });
        }
        return endpoints;
    }

    private generateCrudEndpoints(aggregateName: string, lowerAggregate: string, rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): any[] {
        const dtoType = `${aggregateName}Dto`;
        const createRequestDtoType = `Create${aggregateName}RequestDto`;

        // For create endpoint, use the new CreateXRequestDto which contains:
        // - Full DTOs for cross-aggregate references (e.g., UserDto, CourseDto)
        // - Primitive/value fields specific to this aggregate
        const createParameters: any[] = [
            {
                name: `createRequest`,
                type: createRequestDtoType,
                annotation: '@RequestBody'
            }
        ];

        const endpoints: any[] = [
            {
                method: 'Post',
                path: `/${lowerAggregate}s/create`,
                methodName: `create${aggregateName}`,
                parameters: createParameters,
                returnType: dtoType,
                description: `Create a new ${aggregateName}`,
                isCrud: true,
                isCreate: true
            },
            {
                method: 'Get',
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `get${aggregateName}ById`,
                parameters: [{
                    name: `${lowerAggregate}AggregateId`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                }],
                returnType: dtoType,
                description: `Get ${aggregateName} by aggregate ID`,
                isCrud: true
            },
            {
                method: 'Put',
                path: `/${lowerAggregate}s`,
                methodName: `update${aggregateName}`,
                parameters: [
                    {
                        name: `${lowerAggregate}Dto`,
                        type: dtoType,
                        annotation: '@RequestBody'
                    }
                ],
                returnType: dtoType,
                description: `Update ${aggregateName}`,
                isCrud: true
            },
            {
                method: 'Delete',
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `delete${aggregateName}`,
                parameters: [{
                    name: `${lowerAggregate}AggregateId`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                }],
                returnType: null,
                description: `Delete ${aggregateName}`,
                isCrud: true
            }
        ];

        // Always use getAll (no search methods generated by default)
        endpoints.push({
            method: 'Get',
            path: `/${lowerAggregate}s`,
            methodName: `getAll${aggregateName}s`,
            parameters: [],
            returnType: `List<${dtoType}>`,
            description: `Get all ${aggregateName}s`,
            isCrud: true
        });

        return endpoints;
    }

    private buildControllerImports(aggregate: Aggregate, options: WebApiGenerationOptions, endpoints: any[]): string[] {
        const imports = new Set<string>();

        imports.add('import org.springframework.web.bind.annotation.*;');
        imports.add('import org.springframework.beans.factory.annotation.Autowired;');

        imports.add(`import ${getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'functionalities')}.${aggregate.name}Functionalities;`);

        // Only add collection imports when needed
        const hasListType = endpoints.some(e => e.returnType && e.returnType.includes('List<'));
        if (hasListType) {
            imports.add('import java.util.List;');
        }

        const hasSetType = endpoints.some(e => e.returnType && e.returnType.includes('Set<'));
        if (hasSetType) {
            imports.add('import java.util.Set;');
        }

        const dtoTypes = new Set<string>();
        const enumTypes = new Set<string>();

        endpoints.forEach(endpoint => {
            this.extractDtoTypes(endpoint.returnType, dtoTypes);
            this.extractEnumTypes(endpoint.returnType, enumTypes);

            endpoint.parameters?.forEach((param: any) => {
                this.extractDtoTypes(param.type, dtoTypes);
                this.extractEnumTypes(param.type, enumTypes);
            });
        });

        dtoTypes.forEach(dtoType => {
            const importPath = this.resolveDtoImportPath(dtoType, options);
            if (importPath) {
                imports.add(`import ${importPath};`);
            }
        });

        enumTypes.forEach(enumType => {
            const importPath = this.resolveEnumImportPath(enumType, options);
            if (importPath) {
                imports.add(`import ${importPath};`);
            }
        });

        if (endpoints.some(e => e.throwsException)) {
            imports.add(`import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.*;`);
        }

        // Add imports for request DTOs (Create/Update)
        const hasCreateEndpoint = endpoints.some(e => e.isCreate);
        if (hasCreateEndpoint) {
            const requestDtoPackage = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi', 'requestDtos');
            imports.add(`import ${requestDtoPackage}.Create${aggregate.name}RequestDto;`);
        }

        return Array.from(imports);
    }

    private extractDtoTypes(type: string, dtoSet: Set<string>): void {
        if (!type) return;

        const dtoMatches = type.match(/(\w+Dto)/g);
        if (dtoMatches) {
            dtoMatches.forEach(dto => {
                // Exclude 'Dto' alone and request DTOs (they have their own import)
                if (dto !== 'Dto' && !dto.endsWith('RequestDto')) {
                    dtoSet.add(dto);
                }
            });
        }
    }

    private extractEnumTypes(type: string, enumSet: Set<string>): void {
        if (!type) return;

        const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal', 'void'];
        const typeName = type.replace(/List<|Set<|>/g, '').trim();

        if (typeName &&
            !primitiveTypes.includes(typeName) &&
            !typeName.endsWith('Dto') &&
            !typeName.includes('<') &&
            typeName.charAt(0) === typeName.charAt(0).toUpperCase()) {
            enumSet.add(typeName);
        }
    }

    private resolveEnumImportPath(enumType: string, options: WebApiGenerationOptions): string | null {
        if (!enumType) return null;

        return getGlobalConfig().buildPackageName(options.projectName, 'shared', 'enums') + '.' + enumType;
    }

    /**
     * Find entity relationships (both single and collection entity fields) from root entity properties
     * @deprecated No longer used after simplifying create operations
     */
    // @ts-ignore - deprecated method, kept for potential future use
    private _findEntityRelationships(rootEntity: Entity, aggregate: Aggregate): Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> {
        const relationships: Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type (not enum)
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Resolve entity type
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    // For collections, extract element type
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Only include if it's an entity within this aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                const isEntityInAggregate = !!relatedEntity;

                // Include all entity relationships
                // Note: generateDto flag just means "generate a DTO class", not "exclude from signature"
                if (isEntityInAggregate) {
                    const paramName = prop.name;
                    relationships.push({
                        entityType: entityName,
                        paramName,
                        javaType: isCollection ? javaType : entityName,
                        isCollection
                    });
                }
            }
        }

        return relationships;
    }

    /**
     * Get the related DTO type for an entity relationship
     * @deprecated No longer used after simplifying create operations
     */
    // @ts-ignore - deprecated method, kept for potential future use
    private _getRelatedDtoType(rel: { entityType: string; paramName: string; javaType: string; isCollection: boolean }, aggregate: Aggregate, allAggregates?: Aggregate[]): { dtoType: string | null; isFromAnotherAggregate: boolean; relatedAggregateName?: string } {
        const relatedEntity = aggregate.entities?.find((e: any) => e.name === rel.entityType);
        if (!relatedEntity) return { dtoType: null, isFromAnotherAggregate: false };

        const entityAny = relatedEntity as any;

        // Check if the entity uses an aggregate reference (from "uses Topic")
        const aggregateRef = entityAny.aggregateRef;
        let dtoTypeName: string | null = null;
        let relatedAggregateName: string | undefined = undefined;

        if (aggregateRef) {
            // aggregateRef is the aggregate name (e.g., "Topic"), derive DTO name (e.g., "TopicDto")
            if (typeof aggregateRef === 'string') {
                relatedAggregateName = aggregateRef;
                dtoTypeName = `${aggregateRef}Dto`;
            } else if (aggregateRef.ref?.name) {
                relatedAggregateName = aggregateRef.ref.name;
                dtoTypeName = `${aggregateRef.ref.name}Dto`;
            } else if (aggregateRef.$refText) {
                relatedAggregateName = aggregateRef.$refText;
                dtoTypeName = `${aggregateRef.$refText}Dto`;
            }
        }

        // If entity has generateDto, return the entity name + Dto
        if (!dtoTypeName && entityAny.generateDto) {
            dtoTypeName = `${rel.entityType}Dto`;
        }

        // Check if the DTO is from another aggregate
        if (relatedAggregateName && allAggregates) {
            const targetAggregate = allAggregates.find(agg => agg.name === relatedAggregateName);
            if (targetAggregate && targetAggregate.name !== aggregate.name) {
                return {
                    dtoType: dtoTypeName,
                    isFromAnotherAggregate: true,
                    relatedAggregateName: targetAggregate.name
                };
            }
        }

        return {
            dtoType: dtoTypeName,
            isFromAnotherAggregate: false
        };
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

    async generateEmptyController(aggregate: Aggregate, options: WebApiGenerationOptions): Promise<string> {
        const packageName = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi');

        const context = {
            packageName,
            aggregateName: aggregate.name
        };

        const template = this.getEmptyControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private getControllerTemplate(): string {
        return this.loadTemplate('web/controller.hbs');
    }

    private getEmptyControllerTemplate(): string {
        return this.loadTemplate('web/empty-controller.hbs');
    }

    private resolveDtoImportPath(dtoType: string, options: WebApiGenerationOptions): string | null {
        if (!dtoType || !dtoType.endsWith('Dto')) {
            return null;
        }

        // DTOs are located in shared.dtos package
        const packageName = getGlobalConfig().buildPackageName(
            options.projectName,
            'shared',
            'dtos'
        );
        return `${packageName}.${dtoType}`;
    }
}
