import type { Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { SagaFunctionalityGeneratorBase, SagaOperationMetadata } from '../base/saga-functionality-generator-base.js';



export class SagaReadGenerator extends SagaFunctionalityGeneratorBase {
    protected buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const dtoType = `${capitalizedAggregate}Dto`;

        return {
            operationName: `get${capitalizedAggregate}ById`,
            className: `Get${capitalizedAggregate}ByIdFunctionalitySagas`,
            stepName: `get${capitalizedAggregate}Step`,
            params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
            resultType: dtoType,
            resultField: `${lowerAggregate}Dto`,
            resultSetter: `set${capitalizedAggregate}Dto`,
            resultGetter: `get${capitalizedAggregate}Dto`,
            serviceCall: `${lowerAggregate}Service.get${capitalizedAggregate}ById`,
            serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
        };
    }

    protected buildWorkflowMethod(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();
        const enumName = this.toEnumCase(aggregate.name);
        const buildWorkflowParams = [...metadata.params.map(p => `${p.type} ${p.name}`), 'SagaUnitOfWork unitOfWork'];

        const upperAggregate = capitalizedAggregate.toUpperCase();

        return `    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep ${metadata.stepName} = new SagaStep("${metadata.stepName}", () -> {
            unitOfWorkService.verifySagaState(${lowerAggregate}AggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(${capitalizedAggregate}SagaState.UPDATE_${upperAggregate}, ${capitalizedAggregate}SagaState.DELETE_${upperAggregate})));
            unitOfWorkService.registerSagaState(${lowerAggregate}AggregateId, ${capitalizedAggregate}SagaState.READ_${upperAggregate}, unitOfWork);
            Get${capitalizedAggregate}ByIdCommand cmd = new Get${capitalizedAggregate}ByIdCommand(unitOfWork, ServiceMapping.${enumName}.getServiceName(), ${lowerAggregate}AggregateId);
            ${metadata.resultType} ${metadata.resultField} = (${metadata.resultType}) commandGateway.send(cmd);
            ${metadata.resultSetter}(${metadata.resultField});
        });

        workflow.addStep(${metadata.stepName});
    }`;
    }
}
