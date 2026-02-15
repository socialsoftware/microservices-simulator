import type { Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { SagaFunctionalityGeneratorBase, SagaOperationMetadata } from '../base/saga-functionality-generator-base.js';

/**
 * Generates saga functionality class for read-all operations.
 */
export class SagaReadAllGenerator extends SagaFunctionalityGeneratorBase {
    protected buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const dtoType = `${capitalizedAggregate}Dto`;

        return {
            operationName: `getAll${capitalizedAggregate}s`,
            className: `GetAll${capitalizedAggregate}sFunctionalitySagas`,
            stepName: `getAll${capitalizedAggregate}sStep`,
            params: [],
            resultType: `List<${dtoType}>`,
            resultField: `${lowerAggregate}s`,
            resultSetter: `set${capitalizedAggregate}s`,
            resultGetter: `get${capitalizedAggregate}s`,
            serviceCall: `${lowerAggregate}Service.getAll${capitalizedAggregate}s`,
            serviceArgs: ['unitOfWork']
        };
    }

    protected buildWorkflowMethod(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string {
        const buildWorkflowParams = ['SagaUnitOfWork unitOfWork'];

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
