import { OrchestrationBase } from '../common/orchestration-base.js';
import type { Entity } from '../../../language/generated/ast.js';

export class SagaCrudGenerator extends OrchestrationBase {
    generateCrudSagaFunctionalities(aggregate: any, options: { projectName: string }, packageName: string): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = this.capitalize(aggregate.name);
        const dtoType = `${capitalizedAggregate}Dto`;
        const rootEntity: Entity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name } as any;
        const entityName = rootEntity.name;

        const crudOperations: any[] = [
            {
                name: `create${capitalizedAggregate}`,
                stepName: `create${capitalizedAggregate}Step`,
                params: [{ type: dtoType, name: `${lowerAggregate}Dto` }],
                resultType: dtoType,
                resultField: `created${capitalizedAggregate}Dto`,
                resultSetter: `setCreated${capitalizedAggregate}Dto`,
                resultGetter: `getCreated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.create${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}Dto`, 'unitOfWork']
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
                serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
            },
            {
                name: `update${capitalizedAggregate}`,
                stepName: `update${capitalizedAggregate}Step`,
                params: [
                    { type: 'Integer', name: `${lowerAggregate}AggregateId` },
                    { type: dtoType, name: `${lowerAggregate}Dto` }
                ],
                resultType: dtoType,
                resultField: `updated${capitalizedAggregate}Dto`,
                resultSetter: `setUpdated${capitalizedAggregate}Dto`,
                resultGetter: `getUpdated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.update${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}AggregateId`, `${lowerAggregate}Dto`, 'unitOfWork']
            },
            {
                name: `delete${capitalizedAggregate}`,
                stepName: `delete${capitalizedAggregate}Step`,
                params: [{ type: 'Integer', name: `${lowerAggregate}AggregateId` }],
                resultType: dtoType,
                resultField: `deleted${capitalizedAggregate}Dto`,
                resultSetter: `setDeleted${capitalizedAggregate}Dto`,
                resultGetter: `getDeleted${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.delete${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}AggregateId`, 'unitOfWork']
            }
        ];

        const searchableProperties = this.getSearchableProperties(rootEntity);
        if (searchableProperties.length > 0) {
            const searchParams = searchableProperties.map(prop => ({
                type: prop.type,
                name: prop.name
            }));

            crudOperations.push({
                name: `search${capitalizedAggregate}s`,
                stepName: `search${capitalizedAggregate}sStep`,
                params: searchParams,
                resultType: `List<${dtoType}>`,
                resultField: `searched${entityName}Dtos`,
                resultSetter: `setSearched${entityName}Dtos`,
                resultGetter: `getSearched${entityName}Dtos`,
                serviceCall: `${lowerAggregate}Service.search${capitalizedAggregate}s`,
                serviceArgs: [...searchableProperties.map((p: any) => p.name), 'unitOfWork']
            });
        } else {
            crudOperations.push({
                name: `getAll${capitalizedAggregate}s`,
                stepName: `getAll${capitalizedAggregate}sStep`,
                params: [],
                resultType: `List<${dtoType}>`,
                resultField: `${lowerAggregate}s`,
                resultSetter: `set${capitalizedAggregate}s`,
                resultGetter: `get${capitalizedAggregate}s`,
                serviceCall: `${lowerAggregate}Service.getAll${capitalizedAggregate}s`,
                serviceArgs: ['unitOfWork']
            });
        }

        for (const op of crudOperations) {
            const className = `${this.capitalize(op.name)}FunctionalitySagas`;

            const imports: string[] = [];
            imports.push(`import ${basePackage}.ms.coordination.workflow.WorkflowFunctionality;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.service.${capitalizedAggregate}Service;`);
            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${rootEntity.name}Dto;`);
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

            const isDeleteOperation = op.name.startsWith('delete');
            const isUpdateOperation = op.name.startsWith('update');
            const isGetAllOperation = op.name.startsWith('getAll');
            const isSearchOperation = op.name.startsWith('search');
            if (isDeleteOperation || isUpdateOperation) {
                imports.push('import java.util.ArrayList;');
                imports.push('import java.util.Arrays;');
                imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
                imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
            }
            if (isGetAllOperation || (op.resultType && op.resultType.includes('List<'))) {
                imports.push('import java.util.List;');
            }

            let fieldsDeclaration = '';

            const isGetByIdOperation = op.name === `get${capitalizedAggregate}ById`;
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

            if (op.resultField) {
                fieldsDeclaration += `    private ${op.resultType} ${op.resultField};\n`;
            }

            fieldsDeclaration += `    private final ${capitalizedAggregate}Service ${lowerAggregate}Service;\n`;
            fieldsDeclaration += `    private final SagaUnitOfWorkService unitOfWorkService;`;

            const constructorParams = [
                `${capitalizedAggregate}Service ${lowerAggregate}Service`,
                'SagaUnitOfWorkService unitOfWorkService',
                ...op.params.map((p: any) => `${p.type} ${p.name}`),
                'SagaUnitOfWork unitOfWork'
            ];

            const buildWorkflowParams = [
                ...op.params.map((p: any) => `${p.type} ${p.name}`),
                'SagaUnitOfWork unitOfWork'
            ];

            const buildWorkflowCallArgs = [
                ...op.params.map((p: any) => p.name),
                'unitOfWork'
            ];

            let workflowBody = '';
            const idParamName = op.params[0]?.name || `${lowerAggregate}AggregateId`;

            if (isDeleteOperation) {
                const readMethod = `${lowerAggregate}Service.get${capitalizedAggregate}ById`;
                const readStateConst = `${capitalizedAggregate}SagaState.READ_${capitalizedAggregate.toUpperCase()}`;

                workflowBody = `
        SagaSyncStep get${capitalizedAggregate}Step = new SagaSyncStep(\"get${capitalizedAggregate}Step\", () -> {
            ${op.resultType} ${op.resultField} = ${readMethod}(${idParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
            unitOfWorkService.registerSagaState(${idParamName}, ${readStateConst}, unitOfWork);
        });

        get${capitalizedAggregate}Step.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(${idParamName}, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep delete${capitalizedAggregate}Step = new SagaSyncStep(\"delete${capitalizedAggregate}Step\", () -> {
            ${op.serviceCall}(${op.serviceArgs.join(', ')});
        }, new ArrayList<>(Arrays.asList(get${capitalizedAggregate}Step)));

        workflow.addStep(get${capitalizedAggregate}Step);
        workflow.addStep(delete${capitalizedAggregate}Step);`;
            } else if (isUpdateOperation) {
                const readStateConst = `${capitalizedAggregate}SagaState.READ_${capitalizedAggregate.toUpperCase()}`;
                const dtoParamName = op.params[1]?.name || `${lowerAggregate}Dto`;

                workflowBody = `
        SagaSyncStep get${capitalizedAggregate}Step = new SagaSyncStep(\"get${capitalizedAggregate}Step\", () -> {
            unitOfWorkService.registerSagaState(${idParamName}, ${readStateConst}, unitOfWork);
        });

        get${capitalizedAggregate}Step.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(${idParamName}, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep update${capitalizedAggregate}Step = new SagaSyncStep(\"update${capitalizedAggregate}Step\", () -> {
            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${idParamName}, ${dtoParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
        }, new ArrayList<>(Arrays.asList(get${capitalizedAggregate}Step)));

        workflow.addStep(get${capitalizedAggregate}Step);
        workflow.addStep(update${capitalizedAggregate}Step);`;
            } else {
                let stepBody = '';
                if (op.resultType) {
                    stepBody = `            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${op.serviceArgs.join(', ')});
            ${op.resultSetter}(${op.resultField});`;
                } else {
                    stepBody = `            ${op.serviceCall}(${op.serviceArgs.join(', ')});`;
                }

                workflowBody = `
        SagaSyncStep ${op.stepName} = new SagaSyncStep(\"${op.stepName}\", () -> {
${stepBody}
        });

        workflow.addStep(${op.stepName});`;
            }

            let gettersSettersCode = '';
            if (keepParamFields) {
                for (const param of op.params) {
                    const capitalizedParam = this.capitalize(param.name);
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

            if (op.resultField && op.resultGetter && op.resultSetter) {
                gettersSettersCode += `
    public ${op.resultType} ${op.resultGetter}() {
        return ${op.resultField};
    }

    public void ${op.resultSetter}(${op.resultType} ${op.resultField}) {
        this.${op.resultField} = ${op.resultField};
    }`;
            }

            const content = `package ${packageName};

${imports.join('\n')}

public class ${className} extends WorkflowFunctionality {
${fieldsDeclaration}

    public ${className}(${constructorParams.join(', ')}) {
        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(${buildWorkflowCallArgs.join(', ')});
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

    private getSearchableProperties(entity: any): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = this.resolveJavaType(prop.type);
                    if (!this.isPrimitiveType(javaType) && !this.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                const javaType = this.resolveJavaType(prop.type);
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
    }
}


