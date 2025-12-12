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
        results['coordination'] = await this.generateSagaCoordination(aggregate, rootEntity, options);
        results['workflows'] = await this.generateSagaWorkflows(aggregate, rootEntity, options);

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

    private async generateSagaCoordination(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaCoordinationContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-coordination.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaWorkflows(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaWorkflowsContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-workflows.hbs');
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

            // Check if it's an entity relationship (not a collection, not enum, not primitive)
            // Also check if the type name matches an entity in this aggregate
            const isEntity = this.isEntityType(prop.type) || aggregateEntityNames.has(javaType);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isPrimitive = UnifiedTypeResolver.isPrimitiveType(javaType);
            const isEnum = javaType.endsWith('Type') && javaType !== 'AggregateState';

            if (isEntity && !isCollection && !isPrimitive && !isEnum) {
                relationships.push({
                    type: javaType,
                    name: prop.name,
                    capitalizedName: prop.name.charAt(0).toUpperCase() + prop.name.slice(1)
                });
            }
        }

        return relationships;
    }

    private buildSagaConstructorParams(rootEntity: Entity, relationships: any[], options: SagaGenerationOptions): any {
        const rootEntityName = rootEntity ? rootEntity.name : '';
        const dtoTypeName = `${rootEntityName}Dto`;
        const lowerAggregate = rootEntityName.toLowerCase();

        // Build constructor: (Integer aggregateId, DtoType dto, EntityRelationship...)
        const params: string[] = [];
        params.push(`Integer aggregateId`);
        params.push(`${dtoTypeName} ${lowerAggregate}Dto`);

        for (const rel of relationships) {
            params.push(`${rel.type} ${rel.name}`);
        }

        const paramString = params.join(', ');
        const superCallParams: string[] = [];
        superCallParams.push('aggregateId');
        superCallParams.push(`${lowerAggregate}Dto`);
        for (const rel of relationships) {
            superCallParams.push(rel.name);
        }
        const superCallString = superCallParams.join(', ');

        return {
            paramString,
            superCallString,
            hasRelationships: relationships.length > 0
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

        // Extract saga states from workflows
        const sagaStates = this.extractSagaStatesFromWorkflows(aggregate);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            upperAggregateName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.states`,
            imports,
            sagaStates
        };
    }

    private extractSagaStatesFromWorkflows(aggregate: Aggregate): string[] {
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

        const imports = this.buildSagaFactoriesImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.factories`,
            imports
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

    private buildSagaCoordinationContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaCoordinationImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`,
            imports
        };
    }

    private buildSagaWorkflowsContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = this.buildSagaWorkflowsImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.coordination.${lowerAggregate}`,
            imports
        };
    }

    private buildSagaAggregatesImports(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const basePackage = this.getBasePackage();

        imports.push('import jakarta.persistence.Entity;');

        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);

        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);

        // Add imports for entity relationships
        const relationships = this.extractEntityRelationships(aggregate, rootEntity);
        for (const rel of relationships) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rel.type};`);
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

    private buildSagaFactoriesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;

        imports.push('import org.springframework.context.annotation.Profile;');
        imports.push('import org.springframework.stereotype.Service;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)}Factory;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${this.capitalize(aggregate.name)}Dto;`);

        return imports;
    }

    private buildSagaRepositoriesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import org.springframework.data.jpa.repository.JpaRepository;');
        imports.push('import org.springframework.stereotype.Repository;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${this.capitalize(aggregate.name)};`);

        return imports;
    }

    private buildSagaCoordinationImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;

        imports.push(`import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.service.${this.capitalize(aggregate.name)}Service;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${this.capitalize(aggregate.name)}Dto;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${this.capitalize(aggregate.name)}SagaState;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;`);

        return imports;
    }

    private buildSagaWorkflowsImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push(`import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;`);

        return imports;
    }


}
