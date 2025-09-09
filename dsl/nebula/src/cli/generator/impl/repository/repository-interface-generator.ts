import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { TypeResolver } from "../../base/type-resolver.js";
import { OrchestrationBase } from "../../base/orchestration-base.js";

export interface RepositoryInterfaceGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
    repositoryType?: string;
}

export class RepositoryInterfaceGenerator extends OrchestrationBase {
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
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const queryMethods = this.buildQueryMethods(aggregate, rootEntity, capitalizedAggregate, options);

        const imports = this.buildRepositoryInterfaceImports(aggregate, options, queryMethods);

        const idType = this.detectIdType(rootEntity);
        const repositoryType = this.getRepositoryType(options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`,
            annotations: this.getFrameworkAnnotations(),
            idType,
            repositoryType,
            queryMethods,
            imports,
            hasSagaSupport: options.architecture === 'causal-saga' || options.features?.includes('sagas')
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

        if (options.features?.includes('mongo')) {
            return 'MongoRepository';
        }

        return 'JpaRepository';
    }

    private buildQueryMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, options: RepositoryInterfaceGenerationOptions): any[] {
        const methods: any[] = [];
        const lowerAggregate = aggregateName.toLowerCase();

        rootEntity.properties.forEach((property: any) => {
            if (!property.isKey) {
                const javaType = TypeResolver.resolveJavaType(property.type);

                if (javaType === 'String') {
                    methods.push({
                        methodName: `find${aggregateName}IdBy${this.capitalize(property.name)}`,
                        query: `select ${lowerAggregate}.id from ${aggregateName} ${lowerAggregate} where ${lowerAggregate}.${property.name} = :${property.name} AND ${lowerAggregate}.state = 'ACTIVE'`,
                        parameters: [{ name: property.name, type: javaType }],
                        returnType: 'Optional<Integer>',
                        forSaga: true
                    });
                }
            }
        });

        this.generateRelationshipQueryMethods(aggregate, rootEntity, aggregateName, methods);

        return methods;
    }

    private generateRelationshipQueryMethods(aggregate: Aggregate, rootEntity: Entity, aggregateName: string, methods: any[]): void {
        const lowerAggregate = aggregateName.toLowerCase();

        rootEntity.properties.forEach((property: any) => {
            if (!property.isKey) {
                const referencedAggregate = this.detectReferencedAggregate(property);
                if (referencedAggregate) {
                    const methodName = `find${aggregateName}IdBy${referencedAggregate}IdAnd${this.capitalize(property.name)}`;
                    const query = `select ${lowerAggregate}.id from ${aggregateName} ${lowerAggregate} where ${lowerAggregate}.${property.name}.${referencedAggregate.toLowerCase()}AggregateId = :${referencedAggregate.toLowerCase()}AggregateId AND ${lowerAggregate}.${property.name}.${this.capitalize(property.name)} = :${property.name}`;

                    methods.push({
                        methodName,
                        query,
                        parameters: [
                            { name: `${referencedAggregate.toLowerCase()}AggregateId`, type: 'Integer' },
                            { name: property.name, type: this.capitalize(property.name) }
                        ],
                        returnType: 'Optional<Integer>',
                        forSaga: true
                    });
                }
            }
        });
    }

    private detectReferencedAggregate(property: any): string | null {
        const propertyName = property.name.toLowerCase();
        const javaType = TypeResolver.resolveJavaType(property.type);

        const aggregatePatterns = [
            /(\w+)aggregateid/i,
            /(\w+)id/i,
            /(\w+)aggregate/i
        ];

        for (const pattern of aggregatePatterns) {
            const match = propertyName.match(pattern);
            if (match && match[1]) {
                const potentialAggregate = this.capitalize(match[1]);
                if (this.isValidAggregateName(potentialAggregate)) {
                    return potentialAggregate;
                }
            }
        }

        if (javaType) {
            return this.extractReferencedAggregate(javaType);
        }

        return null;
    }

    private extractReferencedAggregate(javaType: string): string | null {
        const typePatterns = [
            /(\w+)Dto/i,
            /(\w+)(?=Event)/i,
            /(\w+)(?=Id)/i
        ];

        for (const pattern of typePatterns) {
            const match = javaType.match(pattern);
            if (match && match[1]) {
                const potentialAggregate = this.capitalize(match[1]);
                if (this.isValidAggregateName(potentialAggregate)) {
                    return potentialAggregate;
                }
            }
        }

        return null;
    }

    private isValidAggregateName(name: string): boolean {
        const genericTerms = ['id', 'type', 'name', 'value', 'data', 'item', 'object', 'entity', 'dto', 'event'];
        return !genericTerms.includes(name.toLowerCase()) && name.length > 1;
    }

    private buildRepositoryInterfaceImports(aggregate: Aggregate, options: RepositoryInterfaceGenerationOptions, queryMethods: any[]): string[] {
        const imports: string[] = [];
        const repositoryType = this.getRepositoryType(options);

        imports.push('import java.util.Optional;');

        switch (repositoryType) {
            case 'MongoRepository':
                imports.push('import org.springframework.data.mongodb.repository.MongoRepository;');
                imports.push('import org.springframework.data.mongodb.repository.Query;');
                break;
            case 'JpaRepository':
            default:
                imports.push('import org.springframework.data.jpa.repository.JpaRepository;');
                imports.push('import org.springframework.data.jpa.repository.Query;');
                break;
        }

        imports.push('import org.springframework.stereotype.Repository;');
        imports.push('import jakarta.transaction.Transactional;');

        return imports;
    }

    private getRepositoryInterfaceTemplate(): string {
        return this.loadTemplate('repository/repository-interface.hbs');
    }

    protected override renderTemplate(template: string, context: any): string {
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
                const sagaQuery = method.forSaga ?
                    `${method.query} AND ${context.lowerAggregate}.sagaState = 'NOT_IN_SAGA'` :
                    method.query;

                return `    @Query(value = "${sagaQuery}")\n    ${method.returnType} ${method.methodName}ForSaga(${method.parameters.map((p: any) => `${p.type} ${p.name}`).join(', ')});\n\n`;
            }).join('');
            result = result.replace(/\{\{queryMethods\}\}/g, queryMethodsText);
        } else {
            result = result.replace(/\{\{queryMethods\}\}/g, '');
        }

        return result;
    }

}
