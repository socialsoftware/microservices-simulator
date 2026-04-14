import { AggregateExt, EntityExt, TypeGuards } from "../../types/ast-extensions.js";
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from '../common/generator-capabilities.js';
import { UnifiedTypeResolver } from '../common/unified-type-resolver.js';
import { StringUtils } from '../../utils/string-utils.js';

export interface SagaGenerationOptions {
    architecture?: string;
    projectName: string;
    basePackage: string;
}

export class SagaGenerator {
    private capabilities: GeneratorCapabilities;

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createSagaCapabilities();
    }


    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }

    private loadTemplate(templatePath: string): string {
        return templatePath;
    }

    private renderTemplate(templatePath: string, context: any): string {
        return this.capabilities.templateRenderer.render(templatePath, context);
    }

    private resolveJavaType(type: any): string {
        return UnifiedTypeResolver.resolve(type);
    }
    async generateSaga(aggregate: AggregateExt, options: SagaGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e));
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

    private async generateSagaAggregates(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaAggregatesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-aggregate.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaDtos(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaDtosContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-dtos.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaStates(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaStatesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-state.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaFactories(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaFactoriesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-factories.hbs');
        return this.renderTemplate(template, context);
    }

    private async generateSagaRepositories(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaRepositoriesContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('saga/saga-repositories.hbs');
        return this.renderTemplate(template, context);
    }



    private buildSagaAggregatesContext(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaAggregatesImports(aggregate, rootEntity, options);

        
        const entityRelationships = this.extractEntityRelationships(aggregate, rootEntity);
        const constructorParams = this.buildSagaConstructorParams(rootEntity, entityRelationships, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas`,
            basePackageDotProject: `${this.getBasePackage()}.${options.projectName.toLowerCase()}`,
            imports,
            constructorParams
        };
    }

    private extractEntityRelationships(aggregate: AggregateExt, rootEntity: EntityExt): any[] {
        const relationships: any[] = [];

        if (!rootEntity || !rootEntity.properties) {
            return relationships;
        }

        
        const aggregateEntityNames = new Set(aggregate.entities.map((e: EntityExt) => e.name));

        for (const prop of rootEntity.properties) {
            const javaType = this.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isPrimitive = UnifiedTypeResolver.isPrimitiveType(javaType);
            const isEnum = javaType.endsWith('Type') && javaType !== 'AggregateState';

            
            let entityType = javaType;
            if (isCollection) {
                entityType = javaType.replace(/^(Set|List)<(.+)>$/, '$2');
            }

            
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

    private buildSagaConstructorParams(rootEntity: EntityExt, relationships: any[], options: SagaGenerationOptions): any {
        const rootEntityName = rootEntity ? rootEntity.name : '';
        const lowerAggregate = rootEntityName.toLowerCase();
        const dtoTypeName = `${rootEntityName}Dto`;

        
        
        const paramString = `Integer aggregateId, ${dtoTypeName} ${lowerAggregate}Dto`;
        const superCallString = `aggregateId, ${lowerAggregate}Dto`;

        return {
            paramString,
            superCallString,
            hasRelationships: false, 
            relationshipParams: [] 
        };
    }

    private buildSagaDtosContext(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaDtosImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.dtos`,
            imports
        };
    }

    private buildSagaStatesContext(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const upperAggregateName = aggregateName.toUpperCase();

        const imports = this.buildSagaStatesImports(aggregate, options);

        const sagaStates = this.extractSagaStatesFromWorkflows(aggregate, capitalizedAggregate);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            upperAggregateName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.states`,
            imports,
            sagaStates
        };
    }

    private extractSagaStatesFromWorkflows(aggregate: AggregateExt, capitalizedAggregate: string): string[] {
        const states = new Set<string>();

        
        const functionalities = (aggregate as any).functionalities;
        if (functionalities && functionalities.functionalityMethods) {
            for (const func of functionalities.functionalityMethods) {
                const steps = (func as any).functionalitySteps || [];
                for (const step of steps) {
                    
                    const actions = step.stepActions || [];
                    for (const action of actions) {
                        if (action.$type === 'FuncRegisterSagaStateAction' && action.sagaState) {
                            const stateName = String(action.sagaState).trim();
                            const upperState = stateName.toUpperCase();
                            
                            if (upperState !== 'NOT_IN_SAGA' &&
                                upperState !== 'IN_SAGA' &&
                                !stateName.includes('GenericSagaState') &&
                                stateName.length > 0) {
                                states.add(stateName);
                            }
                        }
                    }

                    
                    const compensation = step.compensation;
                    if (compensation && compensation.compensationActions) {
                        for (const compAction of compensation.compensationActions) {
                            if (compAction.$type === 'FuncRegisterSagaStateAction' && compAction.sagaState) {
                                const stateName = String(compAction.sagaState).trim();
                                const upperState = stateName.toUpperCase();
                                
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

        
        return Array.from(states)
            .filter(state => {
                const upper = state.toUpperCase();
                return upper !== 'NOT_IN_SAGA' && upper !== 'IN_SAGA';
            })
            .sort();
    }

    private buildSagaFactoriesContext(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        
        const createMethodParams = `Integer aggregateId, ${rootEntityName}Dto ${lowerAggregate}Dto`;

        const imports = this.buildSagaFactoriesImportsSimplified(aggregate, options);

        
        const constructorCallArgs = `aggregateId, ${lowerAggregate}Dto`;

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.factories`,
            imports,
            createMethodParams,
            constructorCallArgs,
            singleEntityRels: [],
            collectionEntityRels: []
        };
    }

    private buildSagaRepositoriesContext(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = this.buildSagaRepositoriesImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.repositories`,
            imports
        };
    }





    private buildSagaAggregatesImports(aggregate: AggregateExt, rootEntity: EntityExt, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const basePackage = this.getBasePackage();

        imports.push('import jakarta.persistence.Entity;');

        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);
        imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);

        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate};`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);

        return imports;
    }

    private buildSagaDtosImports(aggregate: AggregateExt, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const lowerAggregate = aggregate.name.toLowerCase();

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.${StringUtils.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.${rootEntityName};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate.sagas.Saga${StringUtils.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;`);

        return imports;
    }

    private buildSagaStatesImports(aggregate: AggregateExt, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];
        const basePackage = this.getBasePackage();

        imports.push(`import ${basePackage}.ms.sagas.aggregate.SagaAggregate.SagaState;`);

        return imports;
    }

    

    private buildSagaFactoriesImportsSimplified(aggregate: AggregateExt, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);

        imports.push('import org.springframework.context.annotation.Profile;');
        imports.push('import org.springframework.stereotype.Service;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntityName}Dto;`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${capitalizedAggregate}Factory;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.Saga${capitalizedAggregate};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.dtos.Saga${capitalizedAggregate}Dto;`);

        return imports;
    }

    private buildSagaRepositoriesImports(aggregate: AggregateExt, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import org.springframework.data.jpa.repository.JpaRepository;');
        imports.push('import org.springframework.stereotype.Repository;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.sagas.Saga${StringUtils.capitalize(aggregate.name)};`);

        return imports;
    }






}
