import { OrchestrationBase } from '../common/orchestration-base.js';
import type { Entity, Aggregate } from '../../../language/generated/ast.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';

export class SagaCrudGenerator extends OrchestrationBase {
    generateCrudSagaFunctionalities(aggregate: any, options: { projectName: string }, packageName: string, allAggregates?: Aggregate[]): Record<string, string> {
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
                    { type: dtoType, name: `${lowerAggregate}Dto` }
                ],
                resultType: dtoType,
                resultField: `updated${capitalizedAggregate}Dto`,
                resultSetter: `setUpdated${capitalizedAggregate}Dto`,
                resultGetter: `getUpdated${capitalizedAggregate}Dto`,
                serviceCall: `${lowerAggregate}Service.update${capitalizedAggregate}`,
                serviceArgs: [`${lowerAggregate}Dto`, 'unitOfWork']
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

            // Check if aggregate has cross-aggregate relationships (complex aggregate)
            const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);
            const hasCrossAggregateRelationships = entityRelationships.some(rel => {
                const relatedDtoInfo = this.getRelatedDtoType(rel, aggregate, options, allAggregates);
                return relatedDtoInfo.isFromAnotherAggregate;
            });
            const isSimpleAggregate = !hasCrossAggregateRelationships;

            // Only add these imports if we're using the two-step process (complex aggregates for delete operations)
            // Update operations no longer use the two-step process, so we don't need these imports for updates
            if (isDeleteOperation && !isSimpleAggregate) {
                imports.push('import java.util.ArrayList;');
                imports.push('import java.util.Arrays;');
                imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${capitalizedAggregate}SagaState;`);
                imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
            }
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

            // For simple delete operations, don't add deletedUserDto field
            // Check if it's a delete operation with the deletedDto field pattern
            const isSimpleDeleteWithoutField = isDeleteOperation && isSimpleAggregate &&
                op.resultField && (op.resultField === `${lowerAggregate}Dto` || op.resultField === `deleted${capitalizedAggregate}Dto`);
            if (op.resultField && !isSimpleDeleteWithoutField) {
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
            // For update operations, aggregateId comes from DTO, not a separate param
            const idParamName = isUpdateOperation
                ? (op.params[0]?.name || `${lowerAggregate}Dto`) + '.getAggregateId()'
                : op.params[0]?.name || `${lowerAggregate}AggregateId`;

            if (isDeleteOperation) {
                // Add constructor params for delete operation (after common params)
                constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                // Add unitOfWork only to buildWorkflow params/call args (constructor already has it first)
                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                if (isSimpleAggregate) {
                    // For simple aggregates, call delete directly without get step
                    workflowBody = `
        SagaSyncStep delete${capitalizedAggregate}Step = new SagaSyncStep(\"delete${capitalizedAggregate}Step\", () -> {
            ${op.serviceCall}(${op.serviceArgs.join(', ')});
        });

        workflow.addStep(delete${capitalizedAggregate}Step);
`;
                    // For simple delete operations, we don't need the deletedUserDto field
                    // The field won't be added because of the condition in the field declaration section
                } else {
                    // For complex aggregates, use two-step process
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
        workflow.addStep(delete${capitalizedAggregate}Step);
`;
                }
            } else if (isUpdateOperation) {
                const dtoParamName = op.params[0]?.name || `${lowerAggregate}Dto`;

                // Add constructor params for update operation (after common params)
                constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                // Add unitOfWork only to buildWorkflow params/call args (constructor already has it first)
                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                // For update operations, we can call the service directly without a separate get step
                // The service will handle loading the entity internally
                workflowBody = `
        SagaSyncStep update${capitalizedAggregate}Step = new SagaSyncStep(\"update${capitalizedAggregate}Step\", () -> {
            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${dtoParamName}, unitOfWork);
            ${op.resultSetter}(${op.resultField});
        });

        workflow.addStep(update${capitalizedAggregate}Step);
`;
            } else {
                let stepBody = '';
                let entityCreationCode = '';
                let updatedServiceArgs = op.serviceArgs;
                let crossAggregateSteps: string[] = [];
                let crossAggregateDependencies: string[] = [];

                // For create operations, generate entity relationship objects from DTOs
                if (isCreateOperation) {
                    const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);
                    const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
                    const collectionEntityRels = entityRelationships.filter(rel => rel.isCollection);
                    const dtoParamName = `${lowerAggregate}Dto`;

                    // Collect cross-aggregate aggregateIds to add before DTO
                    const crossAggregateIds: Array<{ paramName: string; relatedAggregateName: string }> = [];

                    // Generate code to create single entity relationships
                    for (const rel of singleEntityRels) {
                        const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                        const dtoGetter = `get${capitalizedRelName}()`;
                        const relatedDtoInfo = this.getRelatedDtoType(rel, aggregate, options, allAggregates);

                        // Add import for entity type
                        const entityPackage = `${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`;
                        imports.push(`import ${entityPackage}.${rel.entityType};`);

                        // If DTO is from another aggregate, generate two-step workflow
                        if (relatedDtoInfo.isFromAnotherAggregate && relatedDtoInfo.relatedAggregateName) {
                            const relatedAggregateName = relatedDtoInfo.relatedAggregateName;
                            const lowerRelatedAggregate = relatedAggregateName.toLowerCase();
                            const capitalizedRelatedAggregate = this.capitalize(relatedAggregateName);
                            const sagaDtoType = `Saga${capitalizedRelatedAggregate}Dto`;
                            const stepName = `get${capitalizedRelatedAggregate}Step`;
                            const sagaStateType = `${capitalizedRelatedAggregate}SagaState`;
                            const relatedDtoVarName = `${lowerRelatedAggregate}Dto`;
                            const aggregateIdParamName = `${lowerRelatedAggregate}AggregateId`;

                            // Add necessary imports
                            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerRelatedAggregate}.service.${capitalizedRelatedAggregate}Service;`);
                            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.dtos.${sagaDtoType};`);
                            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.sagas.aggregates.states.${sagaStateType};`);
                            imports.push(`import ${basePackage}.ms.sagas.aggregate.GenericSagaState;`);
                            imports.push('import java.util.ArrayList;');
                            imports.push('import java.util.Arrays;');

                            // Add fields - DTO for compensation, and entity relationship created in first step
                            fieldsDeclaration += `    private ${sagaDtoType} ${relatedDtoVarName};\n`;
                            fieldsDeclaration += `    private ${rel.entityType} ${rel.paramName};\n`;

                            // Add service dependency
                            fieldsDeclaration += `    private final ${capitalizedRelatedAggregate}Service ${lowerRelatedAggregate}Service;\n`;

                            // Collect aggregateId to add after services
                            crossAggregateIds.push({
                                paramName: aggregateIdParamName,
                                relatedAggregateName: relatedAggregateName
                            });

                            // Update constructor params - add cross-aggregate service after main aggregate service
                            constructorParams.push(`${capitalizedRelatedAggregate}Service ${lowerRelatedAggregate}Service`);

                            // Generate get step - create entity relationship from DTO in the first step
                            const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                            const getStepCode = `
        SagaSyncStep ${stepName} = new SagaSyncStep("${stepName}", () -> {
            ${relatedDtoVarName} = (${sagaDtoType}) ${lowerRelatedAggregate}Service.get${capitalizedRelatedAggregate}ById(${aggregateIdParamName}, unitOfWork);
            unitOfWorkService.registerSagaState(${relatedDtoVarName}.getAggregateId(), ${sagaStateType}.READ_${capitalizedRelatedAggregate.toUpperCase()}, unitOfWork);
            ${rel.entityType} ${rel.paramName} = new ${rel.entityType}(${relatedDtoVarName});
            set${capitalizedRelName}(${rel.paramName});
        });

        ${stepName}.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(${relatedDtoVarName}.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);`;

                            crossAggregateSteps.push(getStepCode);
                            crossAggregateDependencies.push(stepName);

                            // Add getter/setter for the entity relationship
                            gettersSettersCode += `
    public ${rel.entityType} get${capitalizedRelName}() {
        return ${rel.paramName};
    }

    public void set${capitalizedRelName}(${rel.entityType} ${rel.paramName}) {
        this.${rel.paramName} = ${rel.paramName};
    }
`;
                        } else {
                            // Create directly from DTO
                            entityCreationCode += `            ${rel.entityType} ${rel.paramName} = new ${rel.entityType}(${dtoParamName}.${dtoGetter});\n`;
                        }
                    }

                    // Generate code to create collection entity relationships
                    for (const rel of collectionEntityRels) {
                        const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                        const dtoGetter = `get${capitalizedRelName}()`;
                        const elementType = TypeResolver.getElementType(rel.javaType) || rel.javaType.replace(/^(Set|List)<(.+)>$/, '$2');

                        entityCreationCode += `            ${rel.javaType} ${rel.paramName} = ${dtoParamName}.${dtoGetter} != null ? ${dtoParamName}.${dtoGetter}.stream().map(${elementType}::new).collect(java.util.stream.Collectors.to${rel.javaType.startsWith('Set') ? 'Set' : 'List'}()) : null;\n`;

                        // Add imports
                        const entityPackage = `${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`;
                        imports.push(`import ${entityPackage}.${elementType};`);
                        imports.push('import java.util.stream.Collectors;');
                    }

                    // Add cross-aggregate aggregateIds to constructor and buildWorkflow params (after services)
                    for (const aggId of crossAggregateIds) {
                        constructorParams.push(`Integer ${aggId.paramName}`);
                        buildWorkflowParams.push(`Integer ${aggId.paramName}`);
                        buildWorkflowCallArgs.push(aggId.paramName);
                    }

                    // Add DTO to constructor and buildWorkflow params (after ids)
                    constructorParams.push(`${dtoType} ${dtoParamName}`);
                    buildWorkflowParams.push(`${dtoType} ${dtoParamName}`);
                    buildWorkflowCallArgs.push(dtoParamName);

                    // Update service args to include entity relationships
                    if (singleEntityRels.length > 0 || collectionEntityRels.length > 0) {
                        const newServiceArgs: string[] = [];
                        // Single entities first (use getter for cross-aggregate ones created in first step, use param for same-aggregate)
                        for (const rel of singleEntityRels) {
                            const relatedDtoInfo = this.getRelatedDtoType(rel, aggregate, options, allAggregates);
                            if (relatedDtoInfo.isFromAnotherAggregate) {
                                // Use getter for entity relationship created in the first step
                                const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                                newServiceArgs.push(`get${capitalizedRelName}()`);
                            } else {
                                newServiceArgs.push(rel.paramName);
                            }
                        }
                        // Then DTO
                        newServiceArgs.push(dtoParamName);
                        // Then collections
                        for (const rel of collectionEntityRels) {
                            newServiceArgs.push(rel.paramName);
                        }
                        // Finally unitOfWork
                        newServiceArgs.push('unitOfWork');
                        updatedServiceArgs = newServiceArgs;
                    }
                } else {
                    // For non-create operations, use op.params as-is (after common params)
                    constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                }

                // Add unitOfWork to all buildWorkflow signatures (constructor already has it first)
                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                if (op.resultType) {
                    stepBody = `${entityCreationCode}            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${updatedServiceArgs.join(', ')});
            ${op.resultSetter}(${op.resultField});`;
                } else {
                    stepBody = `${entityCreationCode}            ${op.serviceCall}(${updatedServiceArgs.join(', ')});`;
                }

                // Build workflow with cross-aggregate steps
                if (crossAggregateSteps.length > 0) {
                    const dependencies = crossAggregateDependencies.length > 0
                        ? `, new ArrayList<>(Arrays.asList(${crossAggregateDependencies.join(', ')}))`
                        : '';

                    workflowBody = crossAggregateSteps.join('\n') + `

        SagaSyncStep ${op.stepName} = new SagaSyncStep("${op.stepName}", () -> {
${stepBody}
        }${dependencies});

        ${crossAggregateDependencies.map(step => `workflow.addStep(${step});`).join('\n        ')}
        workflow.addStep(${op.stepName});
`;
                } else {
                    workflowBody = `
        SagaSyncStep ${op.stepName} = new SagaSyncStep("${op.stepName}", () -> {
${stepBody}
        });

        workflow.addStep(${op.stepName});
`;
                }
            }

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

            // For simple delete operations, don't generate getters/setters for deletedUserDto
            const isSimpleDeleteWithoutGetters = isDeleteOperation && isSimpleAggregate &&
                op.resultField && (op.resultField === `${lowerAggregate}Dto` || op.resultField === `deleted${capitalizedAggregate}Dto`);
            if (op.resultField && op.resultGetter && op.resultSetter && !isSimpleDeleteWithoutGetters) {
                gettersSettersCode += `
    public ${op.resultType} ${op.resultGetter}() {
        return ${op.resultField};
    }

    public void ${op.resultSetter}(${op.resultType} ${op.resultField}) {
        this.${op.resultField} = ${op.resultField};
    }`;
            }

            // Update constructor body to include cross-aggregate services
            let constructorBody = `        this.${lowerAggregate}Service = ${lowerAggregate}Service;
        this.unitOfWorkService = unitOfWorkService;`;

            // Add cross-aggregate service assignments
            if (isCreateOperation) {
                const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);
                const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
                for (const rel of singleEntityRels) {
                    const relatedDtoInfo = this.getRelatedDtoType(rel, aggregate, options, allAggregates);
                    if (relatedDtoInfo.isFromAnotherAggregate && relatedDtoInfo.relatedAggregateName) {
                        const lowerRelatedAggregate = relatedDtoInfo.relatedAggregateName.toLowerCase();
                        constructorBody += `\n        this.${lowerRelatedAggregate}Service = ${lowerRelatedAggregate}Service;`;
                    }
                }
            }

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

    /**
     * Find entity relationships (both single and collection entity fields) from root entity properties
     */
    private findEntityRelationships(rootEntity: Entity, aggregate: Aggregate): Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> {
        const relationships: Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type (not enum)
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Resolve entity type
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    // For collections, extract element type
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Only include if it's an entity within this aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                const isEntityInAggregate = !!relatedEntity;

                // Exclude DTO entities (entities marked with 'Dto' keyword or generateDto)
                const isDtoEntity = relatedEntity && (relatedEntity as any).generateDto;

                if (isEntityInAggregate && !isDtoEntity) {
                    const paramName = prop.name;
                    relationships.push({
                        entityType: entityName,
                        paramName,
                        javaType: isCollection ? javaType : entityName,
                        isCollection
                    });
                }
            }
        }

        return relationships;
    }

    /**
     * Get the related DTO type for an entity relationship
     */
    private getRelatedDtoType(rel: { entityType: string; paramName: string; javaType: string; isCollection: boolean }, aggregate: Aggregate, options: { projectName: string }, allAggregates?: Aggregate[]): { dtoType: string | null; isFromAnotherAggregate: boolean; relatedAggregateName?: string } {
        const relatedEntity = aggregate.entities?.find((e: any) => e.name === rel.entityType);
        if (!relatedEntity) return { dtoType: null, isFromAnotherAggregate: false };

        const entityAny = relatedEntity as any;

        // Check if the entity uses a DTO type (from "uses dto CourseDto")
        const dtoType = entityAny.dtoType;
        let dtoTypeName: string | null = null;

        if (dtoType) {
            if (dtoType.ref?.name) {
                dtoTypeName = dtoType.ref.name;
            } else if (dtoType.$refText) {
                dtoTypeName = dtoType.$refText;
            } else if (typeof dtoType === 'string') {
                dtoTypeName = dtoType;
            }
        }

        // If entity has generateDto, return the entity name + Dto
        if (!dtoTypeName && entityAny.generateDto) {
            dtoTypeName = `${rel.entityType}Dto`;
        }

        // Check if the DTO is from another aggregate
        if (dtoTypeName && allAggregates) {
            for (const agg of allAggregates) {
                if (agg.name === aggregate.name) continue; // Skip current aggregate

                // Check if this aggregate has a root entity that generates this DTO
                const rootEntity = agg.entities?.find((e: any) => e.isRoot);
                if (rootEntity && rootEntity.name + 'Dto' === dtoTypeName) {
                    return {
                        dtoType: dtoTypeName,
                        isFromAnotherAggregate: true,
                        relatedAggregateName: agg.name
                    };
                }
            }
        }

        return {
            dtoType: dtoTypeName,
            isFromAnotherAggregate: false
        };
    }

    /**
     * Check if a type is an enum
     */
    private isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                return true;
            }
            const ref = type.type.ref;
            if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }
}


