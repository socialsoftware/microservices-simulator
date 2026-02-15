import type { Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { SagaFunctionalityGeneratorBase, SagaOperationMetadata } from '../base/saga-functionality-generator-base.js';



export class SagaCreateGenerator extends SagaFunctionalityGeneratorBase {
    protected buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const dtoType = `${capitalizedAggregate}Dto`;
        const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;

        return {
            operationName: `create${capitalizedAggregate}`,
            className: `Create${capitalizedAggregate}FunctionalitySagas`,
            stepName: `create${capitalizedAggregate}Step`,
            params: [{ type: createRequestDtoType, name: 'createRequest' }],
            resultType: dtoType,
            resultField: `created${capitalizedAggregate}Dto`,
            resultSetter: `setCreated${capitalizedAggregate}Dto`,
            resultGetter: `getCreated${capitalizedAggregate}Dto`,
            serviceCall: `${lowerAggregate}Service.create${capitalizedAggregate}`,
            serviceArgs: ['createRequest', 'unitOfWork']
        };
    }

    protected override buildAdditionalImports(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string[] {
        const basePackage = this.getBasePackage(options);
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;

        return [
            `import ${basePackage}.${options.projectName.toLowerCase()}.coordination.webapi.requestDtos.${createRequestDtoType};`
        ];
    }

    protected buildWorkflowMethod(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string {
        const buildWorkflowParams = [...metadata.params.map(p => `${p.type} ${p.name}`), 'SagaUnitOfWork unitOfWork'];

        return `    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep ${metadata.stepName} = new SagaSyncStep("${metadata.stepName}", () -> {
            ${metadata.resultType} ${metadata.resultField} = ${metadata.serviceCall}(${metadata.serviceArgs.join(', ')});
            ${metadata.resultSetter}(${metadata.resultField});
        });

        workflow.addStep(${metadata.stepName});
    }`;
    }
}
