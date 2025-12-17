import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";

export class ControllerGenerator extends WebApiBaseGenerator {
    async generateController(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildControllerContext(aggregate, rootEntity, options);
        const template = this.getControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private buildControllerContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const endpoints = this.buildEndpoints(rootEntity, capitalizedAggregate, aggregate);
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

    private buildEndpoints(rootEntity: Entity, aggregateName: string, aggregate: Aggregate): any[] {
        const endpoints: any[] = [];
        const lowerAggregate = aggregateName.toLowerCase();

        if (aggregate.webApiEndpoints?.generateCrud) {
            const crudEndpoints = this.generateCrudEndpoints(aggregateName, lowerAggregate, rootEntity);
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

    private generateCrudEndpoints(aggregateName: string, lowerAggregate: string, rootEntity: Entity): any[] {
        const dtoType = `${aggregateName}Dto`;
        const endpoints: any[] = [
            {
                method: 'Post',
                path: `/${lowerAggregate}s/create`,
                methodName: `create${aggregateName}`,
                parameters: [{
                    name: `${lowerAggregate}Dto`,
                    type: dtoType,
                    annotation: '@RequestBody'
                }],
                returnType: dtoType,
                description: `Create a new ${aggregateName}`,
                isCrud: true
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
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `update${aggregateName}`,
                parameters: [
                    {
                        name: `${lowerAggregate}AggregateId`,
                        type: 'Integer',
                        annotation: '@PathVariable'
                    },
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

        const searchableProperties = this.getSearchableProperties(rootEntity);
        if (searchableProperties.length > 0) {
            const searchParams = searchableProperties.map(prop => ({
                name: prop.name,
                type: prop.type,
                annotation: '@RequestParam(required = false)'
            }));

            endpoints.push({
                method: 'Get',
                path: `/${lowerAggregate}s`,
                methodName: `search${aggregateName}s`,
                parameters: searchParams,
                returnType: `List<${dtoType}>`,
                description: `Get all ${aggregateName}s (or filter by parameters)`,
                isCrud: true
            });
        } else {
            endpoints.push({
                method: 'Get',
                path: `/${lowerAggregate}s`,
                methodName: `getAll${aggregateName}s`,
                parameters: [],
                returnType: `List<${dtoType}>`,
                description: `Get all ${aggregateName}s`,
                isCrud: true
            });
        }

        return endpoints;
    }

    private getSearchableProperties(entity: Entity): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = this.resolveParameterType(prop.type);
                    if (!this.isPrimitiveType(javaType) && !this.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                const javaType = this.resolveParameterType(prop.type);
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        // Also expose aggregateId-like fields from entity-type properties (e.g., TopicCourse.courseAggregateId)
        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
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

        return Array.from(imports);
    }

    private extractDtoTypes(type: string, dtoSet: Set<string>): void {
        if (!type) return;

        const dtoMatches = type.match(/(\w+Dto)/g);
        if (dtoMatches) {
            dtoMatches.forEach(dto => {
                if (dto !== 'Dto') {
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
