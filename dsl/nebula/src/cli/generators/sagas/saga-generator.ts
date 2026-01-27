import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { OrchestrationBase } from '../common/orchestration-base.js';
import { UnifiedTypeResolver } from '../common/unified-type-resolver.js';

export interface SagaGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class SagaGenerator extends OrchestrationBase {
    async generateSaga(aggregate: Aggregate, options: SagaGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};
        results['aggregates'] = await this.generateSagaAggregates(aggregate, rootEntity, options);
        results['dtos'] = await this.generateSagaDtos(aggregate, rootEntity, options);
        results['states'] = await this.generateSagaStates(aggregate, rootEntity, options);
        results['factories'] = await this.generateSagaFactories(aggregate, rootEntity, options);
        results['repositories'] = await this.generateSagaRepositories(aggregate, rootEntity, options);

        return results;
    }

    private async generateSagaAggregates(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaAggregatesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-aggregate.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaDtos(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaDtosContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-dtos.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaStates(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaStatesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-state.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaFactories(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaFactoriesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-factories.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaRepositories(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaRepositoriesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-repositories.hbs');
        return this.renderTemplate(template, context);
    }



    private buildSagaAggregatesContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaAggregatesImports(aggregate, rootEntity, options);

        // Extract entity relationships for constructor parameters
        const entityRelationships = this.extractEntityRelationships(aggregate, rootEntity);
        const constructorParams = this.buildSagaConstructorParams(rootEntity, entityRelationships, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates`,
            imports,
            constructorParams
        };
    }

    private extractEntityRelationships(aggregate: Aggregate, rootEntity: Entity): any[] {
        const relationships: any[] = [];

        if (!rootEntity || !rootEntity.properties) {
            return relationships;
        }

        // Build a set of entity names in this aggregate for reference
        const aggregateEntityNames = new Set(aggregate.entities.map((e: Entity) => e.name));

        for (const prop of rootEntity.properties) {
            const javaType = this.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isPrimitive = UnifiedTypeResolver.isPrimitiveType(javaType);
            const isEnum = javaType.endsWith('Type') && javaType !== 'AggregateState';

            // Get the element type for collections
            let entityType = javaType;
            if (isCollection) {
                entityType = javaType.replace(/^(Set|List)<(.+)>$/, '$2');
            }

            // Check if it's an entity within this aggregate
            const isEntityInAggregate = aggregateEntityNames.has(entityType);

            if (isEntityInAggregate && !isPrimitive && !isEnum) {
                relationships.push({
                    type: entityType,
                    javaType: javaType,
                    name: prop.name,
                    capitalizedName: prop.name.charAt(0).toUpperCase() + prop.name.slice(1),
                    isCollection: isCollection
                });
            }
        }

        return relationships;
    }

    private buildSagaConstructorParams(rootEntity: Entity, relationships: any[], options: SagaGenerationOptions): any {
        const rootEntityName = rootEntity ? rootEntity.name : '';
        const lowerAggregate = rootEntityName.toLowerCase();
        const dtoTypeName = `${rootEntityName}Dto`;

        // SIMPLIFIED: Constructor just takes (aggregateId, dto)
        // The base aggregate constructor handles DTO-to-entity conversion internally
        const paramString = `Integer aggregateId, ${dtoTypeName} ${lowerAggregate}Dto`;
        const superCallString = `aggregateId, ${lowerAggregate}Dto`;

        return {
            paramString,
            superCallString,
            hasRelationships: false, // No longer passing relationships as params
            relationshipParams: [] // Empty since we no longer pass relationships
        };
    }

    private buildSagaDtosContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaDtosImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos`,
            imports
        };
    }

    private buildSagaStatesContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const upperAggregateName = aggregateName.toUpperCase();

        const imports = this.buildSagaStatesImports(aggregate, options);

        const sagaStates = this.extractSagaStatesFromWorkflows(aggregate, capitalizedAggregate);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            upperAggregateName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.states`,
            imports,
            sagaStates
        };
    }

    private extractSagaStatesFromWorkflows(aggregate: Aggregate, capitalizedAggregate: string): string[] {
        const states = new Set<string>();

        // Scan functionalities for saga state registrations
        const functionalities = (aggregate as any).functionalities;
        if (functionalities && functionalities.functionalityMethods) {
            for (const func of functionalities.functionalityMethods) {
                const steps = (func as any).functionalitySteps || [];
                for (const step of steps) {
                    // Check step actions
                    const actions = step.stepActions || [];
                    for (const action of actions) {
                        if (action.$type === 'FuncRegisterSagaStateAction' && action.sagaState) {
                            const stateName = String(action.sagaState).trim();
                            const upperState = stateName.toUpperCase();
                            // Only add if it's not a generic state
                            if (upperState !== 'NOT_IN_SAGA' &&
                                upperState !== 'IN_SAGA' &&
                                !stateName.includes('GenericSagaState') &&
                                stateName.length > 0) {
                                states.add(stateName);
                            }
                        }
                    }

                    // Check compensation actions (skip NOT_IN_SAGA from compensations)
                    const compensation = step.compensation;
                    if (compensation && compensation.compensationActions) {
                        for (const compAction of compensation.compensationActions) {
                            if (compAction.$type === 'FuncRegisterSagaStateAction' && compAction.sagaState) {
                                const stateName = String(compAction.sagaState).trim();
                                const upperState = stateName.toUpperCase();
                                // Skip generic states from compensations - these are always GenericSagaState
                                if (upperState !== 'NOT_IN_SAGA' &&
                                    upperState !== 'IN_SAGA' &&
                                    !stateName.includes('GenericSagaState') &&
                                    stateName.length > 0) {
                                    states.add(stateName);
                                }
                            }
                        }
                    }
                }
            }
        }

        const upperName = capitalizedAggregate.toUpperCase();
        states.add(`READ_${upperName}`);
        states.add(`UPDATE_${upperName}`);
        states.add(`DELETE_${upperName}`);

        // Convert to sorted array and ensure no generic states slipped through
        return Array.from(states)
            .filter(state => {
                const upper = state.toUpperCase();
                return upper !== 'NOT_IN_SAGA' && upper !== 'IN_SAGA';
            })
            .sort();
    }

    private buildSagaFactoriesContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        // SIMPLIFIED: Factory interface just takes (aggregateId, dto)
        const createMethodParams = `Integer aggregateId, ${rootEntityName}Dto ${lowerAggregate}Dto`;

        const imports = this.buildSagaFactoriesImportsSimplified(aggregate, options);

        // SIMPLIFIED: Constructor call just passes (aggregateId, dto)
        const constructorCallArgs = `aggregateId, ${lowerAggregate}Dto`;

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.factories`,
            imports,
            createMethodParams,
            constructorCallArgs,
            singleEntityRels: [],
            collectionEntityRels: []
        };
    }

    private buildSagaRepositoriesContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = this.buildSagaRepositoriesImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.repositories`,
            imports
        };
    }





    private buildSagaAggregatesImports(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregate.name);
        const basePackage = this.getBasePackage();

        imports.push('import jakarta.persistence.Entity;');

        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);

        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate};`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);

        // Add imports for entity relationships
        const relationships = this.extractEntityRelationships(aggregate, rootEntity);
        for (const rel of relationships) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rel.type};`);
        }
        
        // Add collection imports if needed
        const hasSet = relationships.some((r: any) => r.isCollection && r.javaType?.startsWith('Set<'));
        const hasList = relationships.some((r: any) => r.isCollection && r.javaType?.startsWith('List<'));
        if (hasSet) {
            imports.push('import java.util.Set;');
        }
        if (hasList) {
            imports.push('import java.util.List;');
        }

        return imports;
    }

    private buildSagaDtosImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;`);

        return imports;
    }

    private buildSagaStatesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];
        const basePackage = this.getBasePackage();

        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);

        return imports;
    }

    /**
     * SIMPLIFIED: Only need basic imports for saga factories
     * Entity relationships are no longer passed as parameters
     */
    private buildSagaFactoriesImportsSimplified(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregate.name);

        imports.push('import org.springframework.context.annotation.Profile;');
        imports.push('import org.springframework.stereotype.Service;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate}Factory;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${capitalizedAggregate};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${capitalizedAggregate}Dto;`);

        return imports;
    }

    private buildSagaRepositoriesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import org.springframework.data.jpa.repository.JpaRepository;');
        imports.push('import org.springframework.stereotype.Repository;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${this.capitalize(aggregate.name)};`);

        return imports;
    }






}
