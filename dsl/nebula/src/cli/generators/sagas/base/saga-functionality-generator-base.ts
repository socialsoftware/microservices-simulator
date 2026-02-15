import type { Entity, Aggregate } from '../../../../language/generated/ast.js';
import { SagaGenerationOptions } from '../saga-generator.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { TypeExtractor } from '../../common/utils/type-extractor.js';

export interface SagaOperationMetadata {
    operationName: string;
    className: string;
    stepName: string;
    params: Array<{ type: string; name: string }>;
    resultType: string | null;
    resultField: string | null;
    resultSetter: string | null;
    resultGetter: string | null;
    serviceCall: string;
    serviceArgs: string[];
}



export abstract class SagaFunctionalityGeneratorBase {
    constructor() {}

    

    generate(
        aggregate: any,
        options: SagaGenerationOptions,
        packageName: string,
        allAggregates?: Aggregate[]
    ): { fileName: string; content: string } {
        const metadata = this.buildOperationMetadata(aggregate, options, allAggregates);
        const imports = this.buildImports(metadata, aggregate, options);
        const fields = this.buildFields(metadata, aggregate);
        const constructor = this.buildConstructor(metadata, aggregate, options);
        const buildWorkflow = this.buildWorkflowMethod(metadata, aggregate, options);
        const gettersSetters = this.buildGettersSetters(metadata, aggregate);

        const content = this.assembleClass(
            packageName,
            metadata.className,
            imports,
            fields,
            constructor,
            buildWorkflow,
            gettersSetters
        );

        return {
            fileName: `${metadata.className}.java`,
            content
        };
    }

    

    protected abstract buildOperationMetadata(
        aggregate: any,
        options: SagaGenerationOptions,
        allAggregates?: Aggregate[]
    ): SagaOperationMetadata;

    

    protected buildImports(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string[] {
        const basePackage = this.getBasePackage(options);
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const rootEntity: Entity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name } as any;

        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
        imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);

        if (metadata.resultType) {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);
        }

        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaSyncStep;`);
        imports.push(`import ${basePackage}.ms.sagas.workflow.SagaWorkflow;`);

        
        const enumTypes = new Set<string>();
        metadata.params.forEach(p => TypeExtractor.extractEnumTypes(p.type, enumTypes));
        if (metadata.resultType) {
            TypeExtractor.extractEnumTypes(metadata.resultType, enumTypes);
        }
        enumTypes.forEach(enumType => {
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.enums.${enumType};`);
        });

        
        if (metadata.resultType && metadata.resultType.includes('List<')) {
            imports.push('import java.util.List;');
        }

        
        const additionalImports = this.buildAdditionalImports(metadata, aggregate, options);
        imports.push(...additionalImports);

        return imports;
    }

    

    protected buildAdditionalImports(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string[] {
        return [];
    }

    

    protected buildFields(metadata: SagaOperationMetadata, aggregate: any): string {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();

        let fields = '';

        
        if (metadata.resultField && metadata.resultType) {
            fields += `    private ${metadata.resultType} ${metadata.resultField};\n`;
        }

        
        fields += `    private final ${capitalizedAggregate}Service ${lowerAggregate}Service;\n`;
        fields += `    private final SagaUnitOfWorkService unitOfWorkService;\n`;

        return fields;
    }

    

    protected buildConstructor(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string {
        const lowerAggregate = aggregate.name.toLowerCase();

        const constructorParams = this.buildConstructorParams(metadata, aggregate);
        const buildWorkflowCallArgs = this.buildWorkflowCallArgs(metadata);

        const constructorBody = `        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(${buildWorkflowCallArgs.join(', ')});`;

        return `    public ${metadata.className}(${constructorParams.join(', ')}) {
${constructorBody}
    }`;
    }

    

    protected buildConstructorParams(metadata: SagaOperationMetadata, aggregate: any): string[] {
        const capitalizedAggregate = StringUtils.capitalize(aggregate.name);
        const lowerAggregate = aggregate.name.toLowerCase();

        const params = [
            'SagaUnitOfWork unitOfWork',
            'SagaUnitOfWorkService unitOfWorkService',
            `${capitalizedAggregate}Service ${lowerAggregate}Service`
        ];

        
        params.push(...metadata.params.map(p => `${p.type} ${p.name}`));

        return params;
    }

    

    protected buildWorkflowCallArgs(metadata: SagaOperationMetadata): string[] {
        const args = [...metadata.params.map(p => p.name), 'unitOfWork'];
        return args;
    }

    

    protected abstract buildWorkflowMethod(metadata: SagaOperationMetadata, aggregate: any, options: SagaGenerationOptions): string;

    

    protected buildGettersSetters(metadata: SagaOperationMetadata, aggregate: any): string {
        if (!metadata.resultField || !metadata.resultType) {
            return '';
        }

        return `
    public ${metadata.resultType} ${metadata.resultGetter}() {
        return ${metadata.resultField};
    }

    public void ${metadata.resultSetter}(${metadata.resultType} ${metadata.resultField}) {
        this.${metadata.resultField} = ${metadata.resultField};
    }`;
    }

    

    protected assembleClass(
        packageName: string,
        className: string,
        imports: string[],
        fields: string,
        constructor: string,
        buildWorkflow: string,
        gettersSetters: string
    ): string {
        return `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${fields}

${constructor}

${buildWorkflow}${gettersSetters}
}
`;
    }

    protected getBasePackage(options: SagaGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in SagaGenerationOptions');
        }
        return options.basePackage;
    }
}
