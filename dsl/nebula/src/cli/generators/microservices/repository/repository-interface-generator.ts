import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { TemplateManager } from "../../../utils/template-manager.js";
import { SpringDataQueryParser } from "./spring-data-query-parser.js";
import { StringUtils } from '../../../utils/string-utils.js';

export interface RepositoryInterfaceGenerationOptions {
    architecture?: string;
    projectName: string;
    basePackage: string;
    repositoryType?: string;
}

export class RepositoryInterfaceGenerator {

    private getBasePackage(options: RepositoryInterfaceGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in RepositoryInterfaceGenerationOptions');
        }
        return options.basePackage;
    }

    private getFrameworkAnnotations(): any {
        return {
            repository: '@Repository',
            transactional: '@Transactional',
            service: '@Service',
            autowired: '@Autowired'
        };
    }

    private loadTemplate(templatePath: string): string {
        const templateManager = TemplateManager.getInstance();
        return templateManager.loadRawTemplate(templatePath);
    }
    async generateRepositoryInterface(aggregate: Aggregate, options: RepositoryInterfaceGenerationOptions): Promise<string> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const context = this.buildRepositoryInterfaceContext(aggregate, rootEntity, options);
        const template = this.getRepositoryInterfaceTemplate();
        return this.renderTemplate(template, context);
    }

    private buildRepositoryInterfaceContext(aggregate: Aggregate, rootEntity: Entity, options: RepositoryInterfaceGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const queryMethods = this.buildQueryMethods(aggregate, rootEntity, capitalizedAggregate, options);

        const imports = this.buildRepositoryInterfaceImports(aggregate, options, queryMethods);

        const idType = this.detectIdType(rootEntity);
        const repositoryType = this.getRepositoryType(options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${this.getBasePackage(options)}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`,
            annotations: this.getFrameworkAnnotations(),
            idType,
            repositoryType,
            queryMethods,
            imports,
            hasSagaSupport: options.architecture === 'causal-saga'
        };
    }

    private detectIdType(rootEntity: Entity): string {
        if (rootEntity.properties) {
            const keyProperty = rootEntity.properties.find((prop: any) => prop.isKey);
            if (keyProperty) {
                return TypeResolver.resolveJavaType(keyProperty.type);
            }
        }

        return 'Integer';
    }

    private getRepositoryType(options: RepositoryInterfaceGenerationOptions): string {
        if (options.repositoryType) {
            return options.repositoryType;
        }

        // Always use JpaRepository (PostgreSQL)
        return 'JpaRepository';
    }

    private buildQueryMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: RepositoryInterfaceGenerationOptions): any[] {
        const methods: any[] = [];

        this.addRepositoryMethods(aggregate, methods);

        return methods;
    }


    /**
     * Maps generated query parameter placeholders to actual method parameter names.
     * Example: ":price" → ":prices" based on method signature
     */
    private mapQueryParameters(parseResult: any, methodParameters: any[]): string {
        let query = parseResult.query;

        // Map each placeholder to the actual parameter name
        for (const mapping of parseResult.parameterMappings) {
            if (mapping.paramIndex < methodParameters.length) {
                const actualParam = methodParameters[mapping.paramIndex];
                const actualParamName = actualParam.name;

                // Replace the placeholder with the actual parameter name
                query = query.replace(new RegExp(mapping.placeholder.replace(':', '\\:'), 'g'), `:${actualParamName}`);
            }
        }

        return query;
    }

    private addRepositoryMethods(aggregate: Aggregate, methods: any[]): void {
        if (aggregate.repository && aggregate.repository.repositoryMethods) {
            const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
            if (!rootEntity) {
                throw new Error(`No root entity found in aggregate ${aggregate.name}`);
            }

            aggregate.repository.repositoryMethods.forEach((method: any) => {
                const returnType = this.resolveRepositoryReturnType(method.returnType);
                const parameters = method.parameters || [];

                const isForSaga = method.name.endsWith('ForSaga');
                const baseMethodName = isForSaga ? method.name.replace(/ForSaga$/, '') : method.name;

                let query = '';
                if (method.query) {
                    // Explicit @Query annotation provided
                    query = method.query;
                } else {
                    // Try to generate query from Spring Data method name conventions
                    const parseResult = SpringDataQueryParser.parseMethodName(
                        baseMethodName,
                        aggregate.name,
                        rootEntity
                    );
                    if (parseResult) {
                        // Map generated parameter placeholders to actual method parameter names
                        query = this.mapQueryParameters(parseResult, parameters);
                        console.log(`    ✨ Auto-generated query for ${baseMethodName}: ${query}`);
                    }
                }

                methods.push({
                    methodName: baseMethodName,
                    query: query,
                    parameters: parameters.map((param: any) => ({
                        name: param.name,
                        type: this.resolveParameterType(param.type)
                    })),
                    returnType: returnType,
                    forSaga: isForSaga
                });
            });
        }
    }

    private resolveRepositoryReturnType(returnType: any): string {
        if (!returnType) return 'void';

        if (returnType.$cstNode && returnType.$cstNode.text) {
            const text = returnType.$cstNode.text.trim();
            if (text) return this.normalizeJavaType(text);
        }

        if (returnType.type) {
            const innerType = this.resolveParameterType(returnType.type);
            const cstText = returnType.$cstNode?.text || '';

            if (cstText.startsWith('Optional<')) {
                return this.normalizeJavaType(`Optional<${innerType}>`);
            } else if (cstText.startsWith('List<')) {
                return this.normalizeJavaType(`List<${innerType}>`);
            } else if (cstText.startsWith('Set<')) {
                return this.normalizeJavaType(`Set<${innerType}>`);
            }

            return this.normalizeJavaType(innerType);
        }

        if (returnType.name) {
            return this.normalizeJavaType(returnType.name);
        }

        return this.normalizeJavaType('Object');
    }

    private resolveParameterType(type: any): string {
        if (!type) return 'Object';

        if (typeof type === 'string') {
            return type;
        }

        if (type.name) {
            return type.name;
        }

        if (type.$type) {
            if (type.$type === 'PrimitiveType' || type.$type === 'BuiltinType' || type.$type === 'EntityType') {
                if (type.name) return type.name;
            }
        }

        if (type.$cstNode && type.$cstNode.text) {
            return type.$cstNode.text;
        }

        return 'Object';
    }

    private normalizeJavaType(typeName: string): string {
        const trimmed = typeName.trim();
        if (trimmed === 'Boolean') {
            return 'boolean';
        }
        return trimmed;
    }


    private buildRepositoryInterfaceImports(aggregate: Aggregate, options: RepositoryInterfaceGenerationOptions, queryMethods: any[]): string[] {
        const imports: string[] = [];
        const repositoryType = this.getRepositoryType(options);

        const hasOptionalReturnType = queryMethods.some(method => method.returnType.includes('Optional<'));
        const hasSetReturnType = queryMethods.some(method => method.returnType.includes('Set<'));
        const hasListReturnType = queryMethods.some(method => method.returnType.includes('List<'));

        if (hasOptionalReturnType) {
            imports.push('import java.util.Optional;');
        }
        if (hasSetReturnType) {
            imports.push('import java.util.Set;');
        }
        if (hasListReturnType) {
            imports.push('import java.util.List;');
        }

        const hasQueryMethods = queryMethods.some(method => method.query && method.query.trim() !== '');

        switch (repositoryType) {
            case 'MongoRepository':
                imports.push('import org.springframework.data.mongodb.repository.MongoRepository;');
                if (hasQueryMethods) {
                    imports.push('import org.springframework.data.mongodb.repository.Query;');
                }
                break;
            case 'JpaRepository':
            default:
                imports.push('import org.springframework.data.jpa.repository.JpaRepository;');
                if (hasQueryMethods) {
                    imports.push('import org.springframework.data.jpa.repository.Query;');
                }
                break;
        }

        imports.push('import org.springframework.stereotype.Repository;');
        imports.push('import jakarta.transaction.Transactional;');

        return imports;
    }

    private getRepositoryInterfaceTemplate(): string {
        return this.loadTemplate('repository/repository-interface.hbs');
    }

    private renderTemplate(template: string, context: any): string {
        let result = template;

        result = result.replace(/\{\{packageName\}\}/g, context.packageName);
        result = result.replace(/\{\{aggregateName\}\}/g, context.aggregateName);
        result = result.replace(/\{\{lowerAggregate\}\}/g, context.lowerAggregate);
        result = result.replace(/\{\{idType\}\}/g, context.idType);
        result = result.replace(/\{\{repositoryType\}\}/g, context.repositoryType);

        if (context.annotations) {
            result = result.replace(/\{\{annotations\.repository\}\}/g, context.annotations.repository);
            result = result.replace(/\{\{annotations\.transactional\}\}/g, context.annotations.transactional);
            result = result.replace(/\{\{annotations\.service\}\}/g, context.annotations.service);
            result = result.replace(/\{\{annotations\.autowired\}\}/g, context.annotations.autowired);
        }

        const importsText = context.imports.map((imp: string) => imp).join('\n');
        result = result.replace(/\{\{imports\}\}/g, importsText);

        if (context.queryMethods && context.queryMethods.length > 0) {
            const queryMethodsText = context.queryMethods.map((method: any) => {
                const methodName = method.forSaga ? `${method.methodName}ForSaga` : method.methodName;
                const methodSignature = `${method.returnType} ${methodName}(${method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ')});`;

                if (method.query && method.query.trim() !== '') {
                    const finalQuery = method.query;
                    return `    @Query(value = "${finalQuery}")
    ${methodSignature}`;
                } else {
                    return `    ${methodSignature}`;
                }
            }).join('\n');
            result = result.replace(/\{\{queryMethods\}\}/g, queryMethodsText);
        } else {
            result = result.replace(/\{\{queryMethods\}\}/g, '');
        }

        return result;
    }

}
