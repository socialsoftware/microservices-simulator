import type { Entity, Aggregate } from '../../../language/generated/ast.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { StringUtils } from '../../utils/string-utils.js';
import { CrudHelpers } from '../common/crud-helpers.js';

export interface CrossAggregateReference {
    entityType: string;
    paramName: string;
    relatedAggregate: string;
    relatedDtoType: string;
    isCollection: boolean;
}

export class SagaCrudGenerator {
    private getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }
    generateCrudSagaFunctionalities(aggregate: any, options: SagaGenerationOptions, packageName: string, allAggregates?: Aggregate[]): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const dtoType = `${capitalizedAggregate}Dto`;
        const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;
        const rootEntity: Entity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name } as any;

        // Find cross-aggregate references for the create operation
        const crossAggregateRefs = CrudHelpers.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

        const crudOperations: any[] = [
            {
                name: `create${capitalizedAggregate}`,
                stepName: `create${capitalizedAggregate}Step`,
                // Use CreateRequestDto instead of aggregate DTO
                params: [{ type: createRequestDtoType, name: `createRequest` }],
                resultType: dtoType,
                resultField: `created${capitalizedAggregate}Dto`,
                resultSetter: `setCreated${capitalizedAggregate}Dto`,
                resultGetter: `getCreated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.create${capitalizedAggregate}`,
                serviceArgs: [], // Will be built dynamically based on cross-aggregate refs
                crossAggregateRefs // Store for use in generation
            },
            {
                name: `get${capitalizedAggregate}ById`,
                stepName: `get${capitalizedAggregate}Step`,
                params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
                resultType: dtoType,
                resultField: `${lowerAggregate}Dto`,
                resultSetter: `set${capitalizedAggregate}Dto`,
                resultGetter: `get${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.get${capitalizedAggregate}ById`,
                serviceArgs: [`${lowerAggregate}AggregateId`]
            },
            {
                name: `update${capitalizedAggregate}`,
                stepName: `update${capitalizedAggregate}Step`,
                params: [
                    { type: dtoType, name: `${lowerAggregate}Dto` }
                ],
                resultType: dtoType,
                resultField: `updated${capitalizedAggregate}Dto`,
                resultSetter: `setUpdated${capitalizedAggregate}Dto`,
                resultGetter: `getUpdated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.update${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}Dto`]
            },
            {
                name: `delete${capitalizedAggregate}`,
                stepName: `delete${capitalizedAggregate}Step`,
                params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
                resultType: null,
                resultField: null,
                resultSetter: null,
                resultGetter: null,
                serviceCall: `${lowerAggregate}Service.delete${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}AggregateId`]
            }
        ];

        // Always use getAll (service doesn't generate search methods by default)
        crudOperations.push({
            name: `getAll${capitalizedAggregate}s`,
            stepName: `getAll${capitalizedAggregate}sStep`,
            params: [],
            resultType: `List<${dtoType}>`,
            resultField: `${lowerAggregate}s`,
            resultSetter: `set${capitalizedAggregate}s`,
            resultGetter: `get${capitalizedAggregate}s`,
            serviceCall: `${lowerAggregate}Service.getAll${capitalizedAggregate}s`,
            serviceArgs: []
        });

        for (const op of crudOperations) {
            const className = `${StringUtils.capitalize(op.name)}FunctionalitySagas`;

            const imports: string[] = [];
            imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);
            const isDeleteOperation = op.name.startsWith('delete');
            if (!isDeleteOperation) {
                imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);
            }
            imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
            imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
            imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
            imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

            const enumTypes = new Set<string>();
            const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal', 'void', 'UnitOfWork'];

            const addEnumTypeIfNeeded = (type: string | undefined) => {
                if (!type) return;
                const typeName = type.replace(/List<|Set<|>/g, '').trim();
                if (!typeName) return;

                if (
                    !primitiveTypes.includes(typeName) &&
                    !typeName.endsWith('Dto') &&
                    !typeName.includes('<') &&
                    typeName.charAt(0) === typeName.charAt(0).toUpperCase()
                ) {
                    enumTypes.add(typeName);
                }
            };

            if (op.params) {
                op.params.forEach((p: any) => addEnumTypeIfNeeded(p.type));
            }
            addEnumTypeIfNeeded(typeof op.resultType === 'string' ? op.resultType : undefined);

            enumTypes.forEach(enumType => {
                const enumImport = `${basePackage}.${options.projectName.toLowerCase()}.shared.enums.${enumType}`;
                imports.push(`import ${enumImport};`);
            });

            const isUpdateOperation = op.name.startsWith('update');
            const isGetAllOperation = op.name.startsWith('getAll');
            const isSearchOperation = op.name.startsWith('search');

            // Delete operations no longer use the two-step process, so we don't need these imports
            // These imports were only needed for the two-step delete process with state registration
            if (isGetAllOperation || (op.resultType && op.resultType.includes('List<'))) {
                imports.push('import java.util.List;');
            }

            let fieldsDeclaration = '';
            let gettersSettersCode = '';

            const isGetByIdOperation = op.name === `get${capitalizedAggregate}ById`;
            const isCreateOperation = op.name.startsWith('create');
            const keepParamFields =
                op.name !== `create${capitalizedAggregate}` &&
                !isDeleteOperation &&
                !isGetByIdOperation &&
                !isUpdateOperation &&
                !isSearchOperation;
            if (keepParamFields) {
                for (const param of op.params) {
                    fieldsDeclaration += `    private ${param.type} ${param.name};\n`;
                }
            }

            // For delete operations, we don't need the deletedDto field since we're not fetching the entity first
            // All delete operations now use a single step without fetching the entity
            if (op.resultField && !isDeleteOperation) {
                fieldsDeclaration += `    private ${op.resultType} ${op.resultField};\n`;
            }

            fieldsDeclaration += `    private final ${capitalizedAggregate}Service ${lowerAggregate}Service;\n`;
            fieldsDeclaration += `    private final SagaUnitOfWorkService unitOfWorkService;\n`;

            // Initialize params - will be built based on operation type.
            // Order: SagaUnitOfWork, SagaUnitOfWorkService, aggregateService, then the rest.
            const constructorParams: string[] = [
                'SagaUnitOfWork unitOfWork',
                'SagaUnitOfWorkService unitOfWorkService',
                `${capitalizedAggregate}Service ${lowerAggregate}Service`
            ];
            const buildWorkflowParams: string[] = [];
            const buildWorkflowCallArgs: string[] = [];

            let workflowBody = '';

            if (isDeleteOperation) {
                // Add constructor params for delete operation (after common params)
                constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                // Add unitOfWork only to buildWorkflow params/call args (constructor already has it first)
                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                // For all delete operations, call delete with (id, unitOfWork)
                const updatedServiceArgs = [...op.serviceArgs, 'unitOfWork'];
                workflowBody = `
        SagaSyncStep delete${capitalizedAggregate}Step = new SagaSyncStep(\"delete${capitalizedAggregate}Step\", () -> {
            ${op.serviceCall}(${updatedServiceArgs.join(', ')});
        });

        workflow.addStep(delete${capitalizedAggregate}Step);
`;
            } else if (isUpdateOperation) {
                const dtoParamName = op.params[0]?.name || `${lowerAggregate}Dto`;

                constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                workflowBody = `
        SagaSyncStep update${capitalizedAggregate}Step = new SagaSyncStep(\"update${capitalizedAggregate}Step\", () -> {
            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${dtoParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
        });

        workflow.addStep(update${capitalizedAggregate}Step);
`;
            } else {
                let stepBody = '';
                let updatedServiceArgs: string[];

                if (isCreateOperation) {
                    const requestParamName = 'createRequest';

                    const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;
                    imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.coordination.webapi.requestDtos.${createRequestDtoType};`);

                    constructorParams.push(`${createRequestDtoType} ${requestParamName}`);
                    buildWorkflowParams.push(`${createRequestDtoType} ${requestParamName}`);
                    buildWorkflowCallArgs.push(requestParamName);

                    updatedServiceArgs = [requestParamName, 'unitOfWork'];
                } else {
                    constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                    // For non-create operations here (getById, getAll), append unitOfWork to service args
                    updatedServiceArgs = [...op.serviceArgs, 'unitOfWork'];
                }

                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                if (op.resultType) {
                    stepBody = `            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${updatedServiceArgs.join(', ')});
            ${op.resultSetter}(${op.resultField});`;
                } else {
                    stepBody = `            ${op.serviceCall}(${updatedServiceArgs.join(', ')});`;
                }

                workflowBody = `
        SagaSyncStep ${op.stepName} = new SagaSyncStep("${op.stepName}", () -> {
${stepBody}
        });

        workflow.addStep(${op.stepName});
`;
            }

            if (keepParamFields) {
                for (const param of op.params) {
                    const capitalizedParam = StringUtils.capitalize(param.name);
                    gettersSettersCode += `
    public ${param.type} get${capitalizedParam}() {
        return ${param.name};
    }

    public void set${capitalizedParam}(${param.type} ${param.name}) {
        this.${param.name} = ${param.name};
    }
`;
                }
            }

            if (op.resultField && op.resultGetter && op.resultSetter && !isDeleteOperation) {
                gettersSettersCode += `
    public ${op.resultType} ${op.resultGetter}() {
        return ${op.resultField};
    }

    public void ${op.resultSetter}(${op.resultType} ${op.resultField}) {
        this.${op.resultField} = ${op.resultField};
    }`;
            }

            // Build constructor body - with the new approach, we don't need cross-aggregate services
            // since the full DTOs are passed directly in the CreateRequestDto
            let constructorBody = `        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;`;

            constructorBody += `\n        this.buildWorkflow(${buildWorkflowCallArgs.join(', ')});`;

            const content = `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${fieldsDeclaration}

    public ${className}(${constructorParams.join(', ')}) {
${constructorBody}
    }

    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
${workflowBody}
    }
${gettersSettersCode}
}
`;

            outputs[className + '.java'] = content;
        }

        return outputs;
    }




}


