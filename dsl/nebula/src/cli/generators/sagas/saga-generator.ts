import { Aggregate, Entity } from "../../../language/generated/ast.js";
import * as fs from 'fs/promises';
import * as path from 'path';
import { fileURLToPath } from 'node:url';
import { OrchestrationBase } from '../shared/orchestration-base.js';
const __dirname = path.dirname(fileURLToPath(import.meta.url));

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
        const template = await this.getSagaAggregatesTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaDtos(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaDtosContext(aggregate, rootEntity, options);
        const template = await this.getSagaDtosTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaStates(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaStatesContext(aggregate, rootEntity, options);
        const template = await this.getSagaStatesTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaFactories(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaFactoriesContext(aggregate, rootEntity, options);
        const template = await this.getSagaFactoriesTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaRepositories(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaRepositoriesContext(aggregate, rootEntity, options);
        const template = await this.getSagaRepositoriesTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaCoordination(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaCoordinationContext(aggregate, rootEntity, options);
        const template = await this.getSagaCoordinationTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateSagaWorkflows(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): Promise<string> {
        const context = this.buildSagaWorkflowsContext(aggregate, rootEntity, options);
        const template = await this.getSagaWorkflowsTemplate();
        return this.renderTemplate(template, context);
    }

    private buildSagaAggregatesContext(aggregate: Aggregate, rootEntity: Entity, options: SagaGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;

        const imports = this.buildSagaAggregatesImports(aggregate, rootEntity, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            rootEntityName: rootEntityName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates`,
            imports
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

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            upperAggregateName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.states`,
            imports
        };
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

        imports.push('import jakarta.persistence.Entity;');

        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName}Dto;`);

        return imports;
    }

    private buildSagaDtosImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName}Dto;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.Saga${this.capitalize(aggregate.name)};`);
        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;');

        return imports;
    }

    private buildSagaStatesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;');

        return imports;
    }

    private buildSagaFactoriesImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregate.name;

        imports.push('import org.springframework.context.annotation.Profile;');
        imports.push('import org.springframework.stereotype.Service;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${this.capitalize(aggregate.name)};`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName}Dto;`);
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

        imports.push('import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;');

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.service.${this.capitalize(aggregate.name)}Service;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${rootEntityName}Dto;`);
        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.Saga${this.capitalize(aggregate.name)}Dto;`);

        imports.push(`import ${this.getBasePackage()}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${this.capitalize(aggregate.name)}SagaState;`);
        imports.push('import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;');

        return imports;
    }

    private buildSagaWorkflowsImports(aggregate: Aggregate, options: SagaGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;');
        imports.push('import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;');

        return imports;
    }

    private async getSagaAggregatesTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-aggregate.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaDtosTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-dtos.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaStatesTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-state.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaFactoriesTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-factories.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaRepositoriesTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-repositories.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaCoordinationTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-coordination.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

    private async getSagaWorkflowsTemplate(): Promise<string> {
        const templatePath = path.join(__dirname, '../../../templates', 'saga', 'saga-workflows.hbs');
        return await fs.readFile(templatePath, 'utf-8');
    }

}
