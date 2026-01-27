import { OrchestrationBase } from '../common/orchestration-base.js';
import type { Entity, Aggregate } from '../../../language/generated/ast.js';
import { TypeResolver } from '../common/resolvers/type-resolver.js';

export interface CrossAggregateReference {
    entityType: string;
    paramName: string;
    relatedAggregate: string;
    relatedDtoType: string;
    isCollection: boolean;
}

export class SagaCrudGenerator extends OrchestrationBase {
    generateCrudSagaFunctionalities(aggregate: any, options: { projectName: string }, packageName: string, allAggregates?: Aggregate[]): Record<string, string> {
        const outputs: Record<string, string> = {};
        const basePackage = this.getBasePackage();
        const lowerAggregate = aggregate.name.toLowerCase();
        const capitalizedAggregate = this.capitalize(aggregate.name);
        const dtoType = `${capitalizedAggregate}Dto`;
        const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;
        const rootEntity: Entity = (aggregate.entities || []).find((e: any) => e.isRoot) || { name: aggregate.name } as any;
        const entityName = rootEntity.name;

        // Find cross-aggregate references for the create operation
        const crossAggregateRefs = this.findCrossAggregateReferences(rootEntity, aggregate, allAggregates);

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

                // For all delete operations, call delete directly without get step
                // Similar to update operations, we don't need to fetch the entity first
                workflowBody = `
        SagaSyncStep delete${capitalizedAggregate}Step = new SagaSyncStep(\"delete${capitalizedAggregate}Step\", () -> {
            ${op.serviceCall}(${op.serviceArgs.join(', ')});
        });

        workflow.addStep(delete${capitalizedAggregate}Step);
`;
                // For simple delete operations, we don't need the deletedUserDto field
                // The field won't be added because of the condition in the field declaration section
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
                let entityCreationCode = '';
                let updatedServiceArgs = op.serviceArgs;

                if (isCreateOperation) {
                    // NEW APPROACH: DTOs are passed directly in CreateRequestDto
                    // No need to fetch from other services - create projection entities directly from the passed DTOs
                    const crossRefs = op.crossAggregateRefs as CrossAggregateReference[] || [];
                    const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);
                    const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
                    const collectionEntityRels = entityRelationships.filter(rel => rel.isCollection);
                    const requestParamName = 'createRequest';

                    // Process single entity relationships
                    for (const rel of singleEntityRels) {
                        const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                        const entityPackage = `${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`;
                        imports.push(`import ${entityPackage}.${rel.entityType};`);

                        // Check if this is a cross-aggregate reference
                        const crossRef = crossRefs.find(cr => cr.paramName === rel.paramName);
                        
                        if (crossRef) {
                            // Cross-aggregate: DTO is passed directly in CreateRequestDto
                            // Create projection entity directly from the passed DTO
                            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${crossRef.relatedDtoType};`);
                            
                            entityCreationCode += `            ${crossRef.relatedDtoType} ${rel.paramName}Dto = ${requestParamName}.get${capitalizedRelName}();\n`;
                            entityCreationCode += `            ${rel.entityType} ${rel.paramName} = new ${rel.entityType}(${rel.paramName}Dto);\n`;
                        } else {
                            // Same-aggregate: create from nested DTO in request
                            const dtoGetter = `get${capitalizedRelName}()`;
                            entityCreationCode += `            ${rel.entityType} ${rel.paramName} = new ${rel.entityType}(${requestParamName}.${dtoGetter});\n`;
                        }
                    }

                    // Process collection entity relationships
                    for (const rel of collectionEntityRels) {
                        const capitalizedRelName = rel.paramName.charAt(0).toUpperCase() + rel.paramName.slice(1);
                        const elementType = TypeResolver.getElementType(rel.javaType) || rel.javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                        const entityPackage = `${basePackage}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.aggregate`;
                        imports.push(`import ${entityPackage}.${elementType};`);

                        // Check if this is a cross-aggregate reference
                        const crossRef = crossRefs.find(cr => cr.paramName === rel.paramName);
                        const collectionType = rel.javaType.startsWith('Set') ? 'Set' : 'List';
                        const collectionImpl = rel.javaType.startsWith('Set') ? 'HashSet' : 'ArrayList';

                        imports.push(`import java.util.${collectionType};`);
                        imports.push(`import java.util.${collectionImpl};`);
                        imports.push('import java.util.stream.Collectors;');

                        if (crossRef) {
                            // Cross-aggregate collection: DTOs are passed directly
                            imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.shared.dtos.${crossRef.relatedDtoType};`);
                            
                            entityCreationCode += `            ${rel.javaType} ${rel.paramName} = null;
            if (${requestParamName}.get${capitalizedRelName}() != null) {
                ${rel.paramName} = ${requestParamName}.get${capitalizedRelName}().stream()
                    .map(${elementType}::new)
                    .collect(Collectors.to${collectionType.charAt(0).toUpperCase() + collectionType.slice(1)}());
            }
`;
                        } else {
                            // Same-aggregate collection: create from nested DTOs
                            const dtoGetter = `get${capitalizedRelName}()`;
                            entityCreationCode += `            ${rel.javaType} ${rel.paramName} = ${requestParamName}.${dtoGetter} != null ? ${requestParamName}.${dtoGetter}.stream().map(${elementType}::new).collect(Collectors.to${collectionType.charAt(0).toUpperCase() + collectionType.slice(1)}()) : null;\n`;
                        }
                    }

                    // Add CreateRequestDto import
                    const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;
                    imports.push(`import ${basePackage}.${options.projectName.toLowerCase()}.coordination.webapi.requestDtos.${createRequestDtoType};`);

                    constructorParams.push(`${createRequestDtoType} ${requestParamName}`);
                    buildWorkflowParams.push(`${createRequestDtoType} ${requestParamName}`);
                    buildWorkflowCallArgs.push(requestParamName);

                    // Build service args: projection entities + request DTO + unitOfWork
                    const newServiceArgs: string[] = [];
                    for (const rel of singleEntityRels) {
                        newServiceArgs.push(rel.paramName);
                    }
                    newServiceArgs.push(requestParamName);
                    for (const rel of collectionEntityRels) {
                        newServiceArgs.push(rel.paramName);
                    }
                    newServiceArgs.push('unitOfWork');
                    updatedServiceArgs = newServiceArgs;
                } else {
                    constructorParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowParams.push(...op.params.map((p: any) => `${p.type} ${p.name}`));
                    buildWorkflowCallArgs.push(...op.params.map((p: any) => p.name));
                }

                buildWorkflowParams.push('SagaUnitOfWork unitOfWork');
                buildWorkflowCallArgs.push('unitOfWork');

                if (op.resultType) {
                    stepBody = `${entityCreationCode}            ${op.resultType} ${op.resultField} = ${op.serviceCall}(${updatedServiceArgs.join(', ')});
            ${op.resultSetter}(${op.resultField});`;
                } else {
                    stepBody = `${entityCreationCode}            ${op.serviceCall}(${updatedServiceArgs.join(', ')});`;
                }

                // With the new approach, we have a single create step that uses the passed DTOs directly
                // No separate fetch steps are needed since DTOs come directly from the CreateRequestDto
                workflowBody = `
        SagaSyncStep ${op.stepName} = new SagaSyncStep("${op.stepName}", () -> {
${stepBody}
        });

        workflow.addStep(${op.stepName});
`;
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

                // Include all entity relationships
                // Note: generateDto flag just means "generate a DTO class", not "exclude from signature"
                if (isEntityInAggregate) {
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
     * Find cross-aggregate references from the root entity's properties.
     * These are non-root entities that have "uses AnotherAggregate" declarations.
     */
    findCrossAggregateReferences(rootEntity: Entity, aggregate: Aggregate, allAggregates?: Aggregate[]): CrossAggregateReference[] {
        const references: CrossAggregateReference[] = [];
        const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);

        for (const rel of entityRelationships) {
            const relatedDtoInfo = this.getRelatedDtoType(rel, aggregate, { projectName: '' }, allAggregates);
            
            if (relatedDtoInfo.isFromAnotherAggregate && relatedDtoInfo.relatedAggregateName) {
                references.push({
                    entityType: rel.entityType,
                    paramName: rel.paramName,
                    relatedAggregate: relatedDtoInfo.relatedAggregateName,
                    relatedDtoType: relatedDtoInfo.dtoType || `${relatedDtoInfo.relatedAggregateName}Dto`,
                    isCollection: rel.isCollection
                });
            }
        }

        return references;
    }

    /**
     * Get the related DTO type for an entity relationship
     */
    private getRelatedDtoType(rel: { entityType: string; paramName: string; javaType: string; isCollection: boolean }, aggregate: Aggregate, options: { projectName: string }, allAggregates?: Aggregate[]): { dtoType: string | null; isFromAnotherAggregate: boolean; relatedAggregateName?: string } {
        const relatedEntity = aggregate.entities?.find((e: any) => e.name === rel.entityType);
        if (!relatedEntity) return { dtoType: null, isFromAnotherAggregate: false };

        const entityAny = relatedEntity as any;

        // Check if the entity uses an aggregate reference (from "uses Topic")
        const aggregateRef = entityAny.aggregateRef;
        let dtoTypeName: string | null = null;
        let relatedAggregateName: string | undefined = undefined;

        if (aggregateRef) {
            // aggregateRef is the aggregate name (e.g., "Topic"), derive DTO name (e.g., "TopicDto")
            if (typeof aggregateRef === 'string') {
                relatedAggregateName = aggregateRef;
                dtoTypeName = `${aggregateRef}Dto`;
            } else if (aggregateRef.ref?.name) {
                relatedAggregateName = aggregateRef.ref.name;
                dtoTypeName = `${aggregateRef.ref.name}Dto`;
            } else if (aggregateRef.$refText) {
                relatedAggregateName = aggregateRef.$refText;
                dtoTypeName = `${aggregateRef.$refText}Dto`;
            }
        }

        // If entity has generateDto, return the entity name + Dto
        if (!dtoTypeName && entityAny.generateDto) {
            dtoTypeName = `${rel.entityType}Dto`;
        }

        // Check if the DTO is from another aggregate
        if (relatedAggregateName && allAggregates) {
            const targetAggregate = allAggregates.find(agg => agg.name === relatedAggregateName);
            if (targetAggregate && targetAggregate.name !== aggregate.name) {
                return {
                    dtoType: dtoTypeName,
                    isFromAnotherAggregate: true,
                    relatedAggregateName: targetAggregate.name
                };
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


