import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../../common/utils/type-constants.js";
import { getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { CollectionProperty } from "./collection-metadata-extractor.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";
import { GeneratorBase } from "../../../common/base/generator-base.js";



export class UpdateMethodGenerator extends GeneratorBase {
    

    generate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string, aggregate: Aggregate): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedIdentifier = this.capitalize(collection.identifierField);

        
        const elementEntity = aggregate.entities?.find((e: any) => e.name === collection.elementType);
        const updatableFieldsWithMapping = this.extractUpdatableFieldsWithMapping(elementEntity, collection.isProjection, collection.identifierField);

        const updateFieldsCode = updatableFieldsWithMapping.map(field => {
            
            const dtoGetterField = this.capitalize(field.dtoFieldName);
            
            const entitySetterField = this.capitalize(field.entityFieldName);

            return `            if (${collection.singularName}Dto.get${dtoGetterField}() != null) {
                element.set${entitySetterField}(${collection.singularName}Dto.get${dtoGetterField}());
            }`;
        }).join('\n');

        
        
        
        let eventConstructorParams: string;
        if (collection.isProjection && elementEntity) {
            
            const allEventParams = this.buildProjectionEventParameters(
                elementEntity,
                lowerEntity,
                collection.identifierField,
                'element'
            );
            eventConstructorParams = allEventParams;
        } else {
            
            eventConstructorParams = `${lowerEntity}Id, ${collection.identifierField}`;
        }

        return `    public ${collection.elementType}Dto update${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new${entityName}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${this.capitalize(projectName)}Exception("${collection.elementType} not found"));
${updateFieldsCode}
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            ${collection.elementType}UpdatedEvent event = new ${collection.elementType}UpdatedEvent(${eventConstructorParams});
            event.setPublisherAggregateVersion(new${entityName}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
${ExceptionGenerator.generateCatchBlock(projectName, 'updating', collection.singularName)}
    }`;
    }

    

    private extractUpdatableFieldsWithMapping(entity: any, isProjection: boolean, identifierField: string): Array<{
        entityFieldName: string;
        dtoFieldName: string;
        type: string;
    }> {
        if (!entity || !entity.properties) {
            return [];
        }

        const fields: Array<{ entityFieldName: string; dtoFieldName: string; type: string }> = [];
        const excludedFields = ['aggregateId', 'version', 'state', identifierField];

        
        if (isProjection && entity.fieldMappings) {

            for (const mapping of entity.fieldMappings) {
                const entityFieldName = mapping.entityField; 
                const dtoFieldName = mapping.dtoField;       

                
                if (entityFieldName.endsWith('AggregateId') ||
                    entityFieldName.endsWith('Version') ||
                    entityFieldName.endsWith('State') ||
                    excludedFields.includes(entityFieldName)) {
                    continue;
                }

                const javaType = TypeResolver.resolveJavaType(mapping.type);

                
                if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                    TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                
                if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                    fields.push({
                        entityFieldName: entityFieldName,
                        dtoFieldName: dtoFieldName,
                        type: javaType
                    });
                }
            }
        } else {
            
            for (const prop of entity.properties) {
                const propName = prop.name;

                
                if (excludedFields.includes(propName)) continue;

                
                if (prop.isFinal) continue;

                
                const javaType = TypeResolver.resolveJavaType(prop.type);

                
                if (javaType.startsWith('Set<') || javaType.startsWith('List<')) {
                    continue;
                }

                
                if (TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                fields.push({
                    entityFieldName: propName,
                    dtoFieldName: propName, 
                    type: javaType
                });
            }
        }

        return fields;
    }

    

    private buildProjectionEventParameters(
        entity: any,
        aggregateVarName: string,
        identifierField: string,
        elementVarName: string
    ): string {
        const params: string[] = [];

        
        params.push(`${aggregateVarName}Id`);

        
        params.push(`${elementVarName}.get${this.capitalize(identifierField)}()`);

        
        
        const prefix = identifierField.replace(/AggregateId$/, '');
        const versionField = `${prefix}Version`;
        params.push(`${elementVarName}.get${this.capitalize(versionField)}()`);

        
        
        
        
        

        
        let fieldsToProcess: Array<{fieldName: string, type: any}> = [];

        if ((entity as any).aggregateRef) {
            
            const fieldMappings = getEffectiveFieldMappings(entity as Entity);
            fieldsToProcess = fieldMappings.map((m: any) => ({
                fieldName: m.entityField,  
                type: m.type
            }));
        } else {
            
            const properties = (entity as any).properties || [];
            fieldsToProcess = properties.map((prop: any) => ({
                fieldName: prop.name,
                type: prop.type
            }));
        }

        for (const field of fieldsToProcess) {
            const fieldName = field.fieldName;

            
            if (fieldName === 'id' ||
                fieldName.endsWith('AggregateId') ||
                fieldName.endsWith('Version') ||
                fieldName.endsWith('State')) {
                continue;
            }

            
            const javaType = TypeResolver.resolveJavaType(field.type);

            
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            
            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${this.capitalize(fieldName)}()`);
            }
        }

        
        for (const prop of entity.properties || []) {
            const propName = prop.name;

            
            if (propName === 'id' ||
                propName.endsWith('AggregateId') ||
                propName.endsWith('Version') ||
                propName.endsWith('State')) {
                continue;
            }

            
            const javaType = TypeResolver.resolveJavaType(prop.type);

            
            if (javaType.startsWith('Set<') ||
                javaType.startsWith('List<') ||
                TypeResolver.isEntityType(javaType)) {
                continue;
            }

            
            const alreadyIncluded = fieldsToProcess.some((m: any) => m.fieldName === propName);
            if (!alreadyIncluded && EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${this.capitalize(propName)}()`);
            }
        }

        return params.join(', ');
    }
}
