import type { Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { SagaFunctionalityGeneratorBase, SagaOperationMetadata } from '../base/saga-functionality-generator-base.js';



export class SagaDeleteGenerator extends SagaFunctionalityGeneratorBase {
    protected buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();

        return {
            operationName: `delete${capitalizedAggregate}`,
            className: `Delete${capitalizedAggregate}FunctionalitySagas`,
            stepName: `delete${capitalizedAggregate}Step`,
            params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            resultType: null,
            resultField: null,
            resultSetter: null,
            resultGetter: null,
            serviceCall: `${lowerAggregate}Service.delete${capitalizedAggregate}`,
            serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
        };
    }

    protected override buildImports(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string[] {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();

        return [
            `import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`,
            `import ${basePackage}.ms.coordination.workflow.CommandGateway;`,
            `import ${basePackage}.${options.projectName.toLowerCase()}.ServiceMapping;`,
            `import ${basePackage}.${options.projectName.toLowerCase()}.command.${lowerAggregate}.*;`,
            `import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`,
            `import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`,
            `import ${basePackage}.ms.sagas.workflow.SagaStep;`,
            `import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`
        ];
    }

    protected buildWorkflowMethod(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const enumName = this.toEnumCase(aggregate.name);
        const buildWorkflowParams = [...metadata.params.map(p => `${p.type} ${p.name}`), 'SagaUnitOfWork unitOfWork'];

        return `    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep ${metadata.stepName} = new SagaStep("${metadata.stepName}", () -> {
            Delete${capitalizedAggregate}Command cmd = new Delete${capitalizedAggregate}Command(unitOfWork, ServiceMapping.${enumName}.getServiceName(), ${lowerAggregate}AggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(${metadata.stepName});
    }`;
    }
}
