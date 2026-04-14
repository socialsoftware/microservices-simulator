import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt } from "../../../../types/ast-extensions.js";
import { capitalize } from "../../../../utils/string-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { getEffectiveFieldMappings } from "../../../../utils/aggregate-helpers.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../../common/utils/type-constants.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";



export class EventHandlerCodeGenerator {


    static generateEventHandlerMethod(
        capitalizedAggregate: string,
        lowerAggregate: string,
        rootEntity: EntityExt,
        projectionEntities: EntityExt[],
        publisherAggregateName: string,
        eventTypeName: string,
        action: 'delete' | 'update',
        projectName: string
    ): string {
        const rootEntityName = rootEntity.name;
        const methodName = `handle${eventTypeName}`;


        const projectionUpdates: string[] = [];

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;




            const aggregateIdField = (projectionEntity.properties || []).find((p: any) =>
                p.name && p.name.toLowerCase() === `${publisherAggregateName.toLowerCase()}aggregateid`
            );



            if (!aggregateIdField) {
                continue;
            }


            const fieldPrefix = (aggregateIdField as any).name.replace(/AggregateId$/, '');
            const capitalizedFieldPrefix = capitalize(fieldPrefix);


            const matchingProperties = rootEntity.properties?.filter(prop => {
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const elementType = TypeResolver.getElementType(prop.type);
                return javaType.includes(projectionEntityName) || elementType === projectionEntityName;
            }) || [];

            for (const prop of matchingProperties) {
                const isCollection = TypeResolver.resolveJavaType(prop.type).startsWith('Set<') ||
                    TypeResolver.resolveJavaType(prop.type).startsWith('List<');
                const propName = prop.name;

                if (isCollection) {

                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE));\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().stream()\n` +
                            `                .filter(item -> item.get${capitalizedFieldPrefix}AggregateId() != null && \n` +
                            `                               item.get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId))\n` +
                            `                .forEach(item -> item.set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version));\n` +
                            `        }`
                        );
                    }
                } else {

                    if (action === 'delete') {
                        projectionUpdates.push(
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}State(Aggregate.AggregateState.INACTIVE);\n` +
                            `        }`
                        );
                    } else if (action === 'update') {
                        projectionUpdates.push(
                            `        if (new${rootEntityName}.get${capitalize(propName)}() != null && \n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId() != null &&\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().get${capitalizedFieldPrefix}AggregateId().equals(${publisherAggregateName.toLowerCase()}AggregateId)) {\n` +
                            `            new${rootEntityName}.get${capitalize(propName)}().set${capitalizedFieldPrefix}Version(${publisherAggregateName.toLowerCase()}Version);\n` +
                            `        }`
                        );
                    }
                }
            }
        }

        const projectionUpdateCode = projectionUpdates.join('\n\n');


        const eventRegistrations: string[] = [];



        const primitiveFieldParams = this.extractPrimitiveFieldsForEvent(projectionEntities, publisherAggregateName, eventTypeName);

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;

            if (action === 'delete') {
                const prefix = publisherAggregateName.toLowerCase();

                eventRegistrations.push(
                    `        unitOfWorkService.registerEvent(\n` +
                    `            new ${projectionEntityName}DeletedEvent(\n` +
                    `                new${rootEntityName}.getAggregateId(),\n` +
                    `                ${prefix}AggregateId\n` +
                    `            ),\n` +
                    `            unitOfWork\n` +
                    `        );`
                );
            } else if (action === 'update') {

                const hasLocalProperties = (projectionEntity.properties || []).some((prop: any) => {
                    const propName = prop.name;

                    if (propName === 'id' || propName === 'aggregateId' ||
                        propName === 'version' || propName === 'state') {
                        return false;
                    }

                    const fieldMappings = (projectionEntity as any).fieldMappings || [];
                    const isFromMapping = fieldMappings.some((m: any) => m.entityField === propName || m.dtoField === propName);
                    return !isFromMapping;
                });



                if (!hasLocalProperties) {
                    const prefix = publisherAggregateName.toLowerCase();


                    const eventParams = [
                        `new${rootEntityName}.getAggregateId()`,
                        `${prefix}AggregateId`,
                        `${prefix}Version`,
                        ...primitiveFieldParams.paramList
                    ].join(',\n                    ');

                    eventRegistrations.push(
                        `        unitOfWorkService.registerEvent(\n` +
                        `            new ${projectionEntityName}UpdatedEvent(\n` +
                        `                    ${eventParams}\n` +
                        `            ),\n` +
                        `            unitOfWork\n` +
                        `        );`
                    );
                }
            }
        }

        const eventRegistrationCode = eventRegistrations.length > 0
            ? '\n' + eventRegistrations.join('\n\n')
            : '';


        const methodParamList = action === 'update'
            ? `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version${primitiveFieldParams.methodSignature}`
            : `Integer aggregateId, Integer ${publisherAggregateName.toLowerCase()}AggregateId, Integer ${publisherAggregateName.toLowerCase()}Version`;

        const methodBody = `            ${rootEntityName} old${rootEntityName} = (${rootEntityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            ${rootEntityName} new${rootEntityName} = ${lowerAggregate}Factory.create${rootEntityName}FromExisting(old${rootEntityName});

${projectionUpdateCode}

            unitOfWorkService.registerChanged(new${rootEntityName}, unitOfWork);
${eventRegistrationCode}

            return new${rootEntityName};`;

        return `    public ${rootEntityName} ${methodName}(${methodParamList}, UnitOfWork unitOfWork) {
${ExceptionGenerator.generateTryCatchWrapper(projectName, `handling ${eventTypeName}`, lowerAggregate, methodBody)}
    }`;
    }



    private static extractPrimitiveFieldsForEvent(projectionEntities: EntityExt[], publisherAggregateName: string, eventTypeName?: string): {
        methodSignature: string;
        paramList: string[];
    } {
        if (projectionEntities.length === 0) {
            return { methodSignature: '', paramList: [] };
        }

        const projectionEntity = projectionEntities[0];

        if (eventTypeName) {
            const eventEntityName = EventNameParser.extractEntityName(eventTypeName);
            const aggregateRef = (projectionEntity as any).aggregateRef;
            if (aggregateRef && eventEntityName.toLowerCase() !== aggregateRef.toLowerCase()) {
                return { methodSignature: '', paramList: [] };
            }
        }

        const entityToExtractFrom = projectionEntity as Entity;

        const fieldMappings = getEffectiveFieldMappings(entityToExtractFrom);

        const fields: Array<{ javaType: string; paramName: string }> = [];

        for (const mapping of fieldMappings) {
            const entityField = mapping.entityField;

            if (entityField.endsWith('AggregateId') || entityField.endsWith('Version') || entityField.endsWith('State')) {
                continue;
            }

            const javaType = TypeResolver.resolveJavaType(mapping.type);

            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType === t || javaType.includes(t))) {
                fields.push({
                    javaType,
                    paramName: entityField
                });
            }
        }


        const methodSignature = fields.length > 0
            ? ', ' + fields.map(f => `${f.javaType} ${f.paramName}`).join(', ')
            : '';


        const paramList = fields.map(f => f.paramName);

        return { methodSignature, paramList };
    }
}
