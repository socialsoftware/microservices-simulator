import type { Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { SagaFunctionalityGeneratorBase, SagaOperationMetadata } from '../base/saga-functionality-generator-base.js';

/**
 * Generates saga functionality class for update operations.
 */
export class SagaUpdateGenerator extends SagaFunctionalityGeneratorBase {
    protected buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const dtoType = `${capitalizedAggregate}Dto`;

        return {
            operationName: `update${capitalizedAggregate}`,
            className: `Update${capitalizedAggregate}FunctionalitySagas`,
            stepName: `update${capitalizedAggregate}Step`,
            params: [{ type: dtoType, name: `${lowerAggregate}Dto` }],
            resultType: dtoType,
            resultField: `updated${capitalizedAggregate}Dto`,
            resultSetter: `setUpdated${capitalizedAggregate}Dto`,
            resultGetter: `getUpdated${capitalizedAggregate}Dto`,
            serviceCall: `${lowerAggregate}Service.update${capitalizedAggregate}`,
            serviceArgs: [`${lowerAggregate}Dto`, 'unitOfWork']
        };
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
