import { Aggregate, Entity } from '../../../language/generated/ast.js';
import { CollectionMetadata, CollectionMetadataBuilder } from '../common/utils/collection-metadata-builder.js';
import { SagaGenerationOptions } from './saga-generator.js';
import { StringUtils } from '../../utils/string-utils.js';

export class SagaCollectionGenerator {
    private getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }
    

    generateCollectionSagaFunctionalities(
        aggregate: Aggregate,
        rootEntity: Entity,
        options: SagaGenerationOptions,
        packageName: string
    ): Record<string, string> {
        const outputs: Record<string, string> = {};
        const collections = CollectionMetadataBuilder.extractCollections(aggregate, rootEntity);

        for (const collection of collections) {
            const collectionSagas = this.generateSagasForCollection(
                collection,
                aggregate,
                rootEntity,
                options,
                packageName
            );
            Object.assign(outputs, collectionSagas);
        }

        return outputs;
    }

    

    private generateSagasForCollection(
        collection: CollectionMetadata,
        aggregate: Aggregate,
        rootEntity: Entity,
        options: SagaGenerationOptions,
        packageName: string
    ): Record<string, string> {
        const outputs: Record<string, string> = {};
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();

        
        const sagaSpecs = [
            {
                operation: 'add',
                name: `add${aggregateName}${collection.capitalizedSingular}`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}Id` },
                    { type: collection.identifierType, name: collection.identifierField },
                    { type: collection.elementDtoType, name: `${collection.singularName}Dto` }
                ],
                resultType: collection.elementDtoType,
                resultField: `added${collection.capitalizedSingular}Dto`,
                resultGetter: `getAdded${collection.capitalizedSingular}Dto`,
                stepName: `add${collection.capitalizedSingular}Step`
            },
            {
                operation: 'addBatch',
                name: `add${aggregateName}${collection.capitalizedSingular}s`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}Id` },
                    { type: `List<${collection.elementDtoType}>`, name: `${collection.singularName}Dtos` }
                ],
                resultType: `List<${collection.elementDtoType}>`,
                resultField: `added${collection.capitalizedSingular}Dtos`,
                resultGetter: `getAdded${collection.capitalizedSingular}Dtos`,
                stepName: `add${collection.capitalizedSingular}sStep`
            },
            {
                operation: 'get',
                name: `get${aggregateName}${collection.capitalizedSingular}`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}Id` },
                    { type: collection.identifierType, name: collection.identifierField }
                ],
                resultType: collection.elementDtoType,
                resultField: `${collection.singularName}Dto`,
                resultGetter: `get${collection.capitalizedSingular}Dto`,
                stepName: `get${collection.capitalizedSingular}Step`
            },
            {
                operation: 'update',
                name: `update${aggregateName}${collection.capitalizedSingular}`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}Id` },
                    { type: collection.identifierType, name: collection.identifierField },
                    { type: collection.elementDtoType, name: `${collection.singularName}Dto` }
                ],
                resultType: collection.elementDtoType,
                resultField: `updated${collection.capitalizedSingular}Dto`,
                resultGetter: `getUpdated${collection.capitalizedSingular}Dto`,
                stepName: `update${collection.capitalizedSingular}Step`
            },
            {
                operation: 'remove',
                name: `remove${aggregateName}${collection.capitalizedSingular}`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}Id` },
                    { type: collection.identifierType, name: collection.identifierField }
                ],
                resultType: null,
                resultField: null,
                resultGetter: null,
                stepName: `remove${collection.capitalizedSingular}Step`
            }
        ];

        for (const spec of sagaSpecs) {
            const className = `${StringUtils.capitalize(spec.name)}FunctionalitySagas`;
            const content = this.generateSagaClass(
                spec,
                aggregateName,
                lowerAggregate,
                collection,
                options,
                packageName
            );
            outputs[`${className}.java`] = content;
        }

        return outputs;
    }

    

    private generateSagaClass(
        spec: any,
        aggregateName: string,
        lowerAggregate: string,
        collection: CollectionMetadata,
        options: SagaGenerationOptions,
        packageName: string
    ): string {
        const basePackage = this.getBasePackage(options);
        const className = `${StringUtils.capitalize(spec.name)}FunctionalitySagas`;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);

        
        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);

        if (spec.resultType) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${collection.elementDtoType};`);
        }

        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

        if (spec.resultType?.includes('List<')) {
            imports.push('import java.util.List;');
        }

        
        const fields: string[] = [];
        if (spec.resultType) {
            fields.push(`    private ${spec.resultType} ${spec.resultField};`);
        }
        fields.push(`    private final ${capitalizedAggregate}Service ${lowerAggregate}Service;`);
        fields.push(`    private final SagaUnitOfWorkService unitOfWorkService;`);

        
        const constructorParams = [
            'SagaUnitOfWork unitOfWork',
            'SagaUnitOfWorkService unitOfWorkService',
            `${capitalizedAggregate}Service ${lowerAggregate}Service`,
            ...spec.params.map((p: any) => `${p.type} ${p.name}`)
        ];

        
        const buildWorkflowParams = [
            ...spec.params.map((p: any) => `${p.type} ${p.name}`),
            'SagaUnitOfWork unitOfWork'
        ];

        const buildWorkflowCallArgs = [
            ...spec.params.map((p: any) => p.name),
            'unitOfWork'
        ];

        
        
        
        const includeAggregateName = collection.isProjection;
        const operationVerb = spec.operation === 'addBatch'
            ? (includeAggregateName ? `add${capitalizedAggregate}${collection.capitalizedSingular}s` : `add${collection.capitalizedSingular}s`)
            : (includeAggregateName ? `${spec.operation}${capitalizedAggregate}${collection.capitalizedSingular}` : `${spec.operation}${collection.capitalizedSingular}`);
        const serviceMethodName = operationVerb;
        const serviceArgs = [...spec.params.map((p: any) => p.name), 'unitOfWork'];

        let stepBody: string;
        if (spec.resultType) {
            const setterName = `set${StringUtils.capitalize(spec.resultField)}`;
            stepBody = `            ${spec.resultType} ${spec.resultField} = ${lowerAggregate}Service.${serviceMethodName}(${serviceArgs.join(', ')});
            ${setterName}(${spec.resultField});`;
        } else {
            stepBody = `            ${lowerAggregate}Service.${serviceMethodName}(${serviceArgs.join(', ')});`;
        }

        const workflowBody = `
        SagaSyncStep ${spec.stepName} = new SagaSyncStep("${spec.stepName}", () -> {
${stepBody}
        });

        workflow.addStep(${spec.stepName});`;

        
        let getterSetter = '';
        if (spec.resultField && spec.resultGetter) {
            getterSetter = `
    public ${spec.resultType} ${spec.resultGetter}() {
        return ${spec.resultField};
    }

    public void set${spec.resultGetter.substring(3)}(${spec.resultType} ${spec.resultField}) {
        this.${spec.resultField} = ${spec.resultField};
    }`;
        }

        
        return `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${fields.join('\n')}

    public ${className}(${constructorParams.join(', ')}) {
        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(${buildWorkflowCallArgs.join(', ')});
    }

    public void buildWorkflow(${buildWorkflowParams.join(', ')}) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
${workflowBody}
    }${getterSetter}
}
`;
    }
}
