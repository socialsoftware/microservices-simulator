import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { CollectionMetadataExtractor, CollectionProperty } from "../collection/collection-metadata-extractor.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../../common/utils/type-constants.js";
import { getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";
import { capitalize } from "../../../../utils/string-utils.js";

export class ServiceCollectionGenerator {
    static generateCollectionMethods(aggregateName: string, rootEntity: Entity, projectName: string, aggregate?: Aggregate): string {
        if (!rootEntity.properties || !aggregate) {
            return '';
        }

        const collections = CollectionMetadataExtractor.findCollectionProperties(rootEntity, aggregate);
        const methods: string[] = [];

        for (const collection of collections) {
            methods.push(this.generateAdd(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateAddBatch(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateGet(collection, rootEntity, projectName));
            methods.push(this.generateRemove(collection, aggregateName, rootEntity, projectName));
            methods.push(this.generateUpdate(collection, aggregateName, rootEntity, projectName, aggregate));
        }

        return methods.join('\n\n');
    }

    private static generateAdd(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();
        const catchBlock = ExceptionGenerator.generateCatchBlock(projectName, 'adding', collection.singularName);

        return `    public ${collection.elementType}Dto add${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new ${collection.elementType}(${collection.singularName}Dto);
            element.set${entityName}(new${entityName});
            new${entityName}.get${collection.capitalizedCollection}().add(element);
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            return ${collection.singularName}Dto;
${catchBlock}
    }`;
    }

    private static generateAddBatch(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();

        return `    public List<${collection.elementType}Dto> add${collection.capitalizedSingular}s(Integer ${lowerEntity}Id, List<${collection.elementType}Dto> ${collection.singularName}Dtos, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.singularName}Dtos.forEach(dto -> {
                ${collection.elementType} element = new ${collection.elementType}(dto);
                element.set${entityName}(new${entityName});
                new${entityName}.get${collection.capitalizedCollection}().add(element);
            });
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            return ${collection.singularName}Dtos;
${ExceptionGenerator.generateCatchBlock(projectName, 'adding', `${collection.singularName}s`)}
    }`;
    }

    private static generateGet(collection: CollectionProperty, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        return `    public ${collection.elementType}Dto get${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} ${lowerEntity} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${collection.elementType} element = ${lowerEntity}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${collection.elementType} not found"));
            return element.buildDto();
${ExceptionGenerator.generateCatchBlock(projectName, 'retrieving', collection.singularName)}
    }`;
    }

    private static generateRemove(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        return `    public void remove${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            new${entityName}.get${collection.capitalizedCollection}().removeIf(item ->
                item.get${capitalizedIdentifier}() != null &&
                item.get${capitalizedIdentifier}().equals(${collection.identifierField})
            );
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            ${collection.elementType}RemovedEvent event = new ${collection.elementType}RemovedEvent(${lowerEntity}Id, ${collection.identifierField});
            event.setPublisherAggregateVersion(new${entityName}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
${ExceptionGenerator.generateCatchBlock(projectName, 'removing', collection.singularName)}
    }`;
    }

    private static generateUpdate(collection: CollectionProperty, aggregateName: string, rootEntity: Entity, projectName: string, aggregate: Aggregate): string {
        const entityName = rootEntity.name;
        const lowerEntity = entityName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedIdentifier = capitalize(collection.identifierField);

        const elementEntity = aggregate.entities?.find((e: any) => e.name === collection.elementType);
        const updatableFieldsWithMapping = this.extractUpdatableFields(elementEntity, collection.isProjection, collection.identifierField);

        const updateFieldsCode = updatableFieldsWithMapping.map(field => {
            const dtoGetterField = capitalize(field.dtoFieldName);
            const entitySetterField = capitalize(field.entityFieldName);
            return `            if (${collection.singularName}Dto.get${dtoGetterField}() != null) {
                element.set${entitySetterField}(${collection.singularName}Dto.get${dtoGetterField}());
            }`;
        }).join('\n');

        const eventConstructorParams = collection.isProjection && elementEntity
            ? this.buildProjectionEventParameters(elementEntity, lowerEntity, collection.identifierField, 'element')
            : `${lowerEntity}Id, ${collection.identifierField}`;

        return `    public ${collection.elementType}Dto update${collection.capitalizedSingular}(Integer ${lowerEntity}Id, Integer ${collection.identifierField}, ${collection.elementType}Dto ${collection.singularName}Dto, UnitOfWork unitOfWork) {
        try {
            ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${lowerEntity}Id, unitOfWork);
            ${entityName} new${entityName} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${entityName});
            ${collection.elementType} element = new${entityName}.get${collection.capitalizedCollection}().stream()
                .filter(item -> item.get${capitalizedIdentifier}() != null &&
                               item.get${capitalizedIdentifier}().equals(${collection.identifierField}))
                .findFirst()
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${collection.elementType} not found"));
${updateFieldsCode}
            unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
            ${collection.elementType}UpdatedEvent event = new ${collection.elementType}UpdatedEvent(${eventConstructorParams});
            event.setPublisherAggregateVersion(new${entityName}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
${ExceptionGenerator.generateCatchBlock(projectName, 'updating', collection.singularName)}
    }`;
    }

    private static extractUpdatableFields(entity: any, isProjection: boolean, identifierField: string): Array<{ entityFieldName: string; dtoFieldName: string; type: string }> {
        if (!entity || !entity.properties) return [];

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
                if (javaType.startsWith('Set<') || javaType.startsWith('List<') || TypeResolver.isEntityType(javaType)) {
                    continue;
                }

                if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                    fields.push({ entityFieldName, dtoFieldName, type: javaType });
                }
            }
        } else {
            for (const prop of entity.properties) {
                const propName = prop.name;
                if (excludedFields.includes(propName)) continue;
                if (prop.isFinal) continue;

                const javaType = TypeResolver.resolveJavaType(prop.type);
                if (javaType.startsWith('Set<') || javaType.startsWith('List<')) continue;
                if (TypeResolver.isEntityType(javaType)) continue;

                fields.push({ entityFieldName: propName, dtoFieldName: propName, type: javaType });
            }
        }

        return fields;
    }

    private static buildProjectionEventParameters(entity: any, aggregateVarName: string, identifierField: string, elementVarName: string): string {
        const params: string[] = [];
        params.push(`${aggregateVarName}Id`);
        params.push(`${elementVarName}.get${capitalize(identifierField)}()`);

        const prefix = identifierField.replace(/AggregateId$/, '');
        const versionField = `${prefix}Version`;
        params.push(`${elementVarName}.get${capitalize(versionField)}()`);

        let fieldsToProcess: Array<{ fieldName: string; type: any }> = [];

        if ((entity as any).aggregateRef) {
            const fieldMappings = getEffectiveFieldMappings(entity as Entity);
            fieldsToProcess = fieldMappings.map((m: any) => ({ fieldName: m.entityField, type: m.type }));
        } else {
            const properties = (entity as any).properties || [];
            fieldsToProcess = properties.map((prop: any) => ({ fieldName: prop.name, type: prop.type }));
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
            if (javaType.startsWith('Set<') || javaType.startsWith('List<') || TypeResolver.isEntityType(javaType)) {
                continue;
            }

            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${capitalize(fieldName)}()`);
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
            if (javaType.startsWith('Set<') || javaType.startsWith('List<') || TypeResolver.isEntityType(javaType)) {
                continue;
            }

            const alreadyIncluded = fieldsToProcess.some((m: any) => m.fieldName === propName);
            if (!alreadyIncluded && EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                params.push(`${elementVarName}.get${capitalize(propName)}()`);
            }
        }

        return params.join(', ');
    }
}
