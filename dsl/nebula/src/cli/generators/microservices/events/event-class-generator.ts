import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, PublishedEventContext } from "./event-types.js";
import { EXTENDED_PRIMITIVE_TYPES } from "../../common/utils/type-constants.js";
import { getEffectiveFieldMappings } from "../../../utils/aggregate-helpers.js";

export class PublishedEventGenerator extends EventBaseGenerator {
    async generatePublishedEvents(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildPublishedEventsContext(aggregate, rootEntity, options);

        for (const event of context.publishedEvents) {
            const eventContext = {
                ...context,
                event
            };

            const eventKey = event.fullEventName || event.capitalizedEventName || event.eventName;
            results[`published-event-${eventKey}`] = await this.generateIndividualPublishedEvent(eventContext);
        }

        return results;
    }

    private buildPublishedEventsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): PublishedEventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const rootEntityEvents = this.buildPublishedEvents(rootEntity, baseContext.aggregateName);
        const projectionEntityEvents = this.buildProjectionEntityEvents(aggregate, baseContext.aggregateName);
        const collectionManipulationEvents = this.buildCollectionManipulationEvents(aggregate, rootEntity);
        const publishedEvents = [...rootEntityEvents, ...projectionEntityEvents, ...collectionManipulationEvents];
        const imports = this.buildPublishedEventsImports(aggregate, options, publishedEvents);

        return {
            ...baseContext,
            publishedEvents,
            imports
        };
    }

    private buildPublishedEvents(rootEntity: Entity, aggregateName: string): any[] {
        const eventTypes = ['Updated', 'Deleted'];

        return eventTypes.map(eventType => {
            const variations = this.getEventNameVariations(eventType, aggregateName);
            let properties: any[] = [];

            if (eventType === 'Updated') {
                const allProperties = this.buildEventProperties(rootEntity, eventType);
                const rootProps = (rootEntity.properties || []) as any[];

                properties = allProperties.filter((prop: any) => {
                    const rootProp = rootProps.find(p => p.name === prop.name);
                    const typeNode = rootProp?.type;

                    let isEntityRef = false;
                    let isCollectionRef = false;
                    if (typeNode && typeof typeNode === 'object') {
                        const t: any = typeNode;
                        if (t.$type === 'EntityType') {
                            isEntityRef = true;
                        } else if (t.$type === 'ListType' || t.$type === 'SetType') {
                            isCollectionRef = true;
                        }
                    }

                    return (
                        !prop.isFinal &&
                        !prop.isCollection &&
                        !prop.isEntity &&
                        !isEntityRef &&
                        !isCollectionRef
                    );
                });
            } else if (eventType === 'Deleted') {
                properties = [];
            }

            return {
                eventType,
                ...variations,
                properties,
                timestamp: new Date().toISOString()
            };
        });
    }

    private buildProjectionEntityEvents(aggregate: Aggregate, aggregateName: string): any[] {
        const projectionEvents: any[] = [];


        const projectionEntities = (aggregate.entities || []).filter((e: any) =>
            !e.isRoot && e.aggregateRef
        );

        for (const projectionEntity of projectionEntities) {
            const projectionEntityName = projectionEntity.name;
            const sourceAggregateName = (projectionEntity as any).aggregateRef;
            const prefix = sourceAggregateName.toLowerCase();


            const deletedEventVariations = this.getEventNameVariations('Deleted', projectionEntityName);
            const deletedFields = [
                {
                    name: `${prefix}AggregateId`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}AggregateId`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                }
            ];

            projectionEvents.push({
                eventType: 'Deleted',
                ...deletedEventVariations,
                properties: deletedFields,
                isProjectionEvent: true,
                projectionEntityName,
                sourceAggregateName,
                timestamp: new Date().toISOString()
            });


            const updatedEventVariations = this.getEventNameVariations('Updated', projectionEntityName);
            const updatedFields = [
                {
                    name: `${prefix}AggregateId`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}AggregateId`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                },
                {
                    name: `${prefix}Version`,
                    type: 'Integer',
                    capitalizedName: this.capitalize(`${prefix}Version`),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                }
            ];


            const primitiveFields = this.extractPrimitiveProjectionFields(projectionEntity, prefix);
            updatedFields.push(...primitiveFields);

            projectionEvents.push({
                eventType: 'Updated',
                ...updatedEventVariations,
                properties: updatedFields,
                isProjectionEvent: true,
                projectionEntityName,
                sourceAggregateName,
                timestamp: new Date().toISOString()
            });
        }

        return projectionEvents;
    }



    private buildCollectionManipulationEvents(aggregate: Aggregate, rootEntity: Entity): any[] {
        const collectionEvents: any[] = [];

        if (!rootEntity.properties) {
            return collectionEvents;
        }


        for (const prop of rootEntity.properties) {
            const propType = (prop as any).type;


            if (propType && (propType.$type === 'SetType' || propType.$type === 'ListType')) {
                const elementType = propType.elementType?.type?.ref?.name || propType.elementType?.type?.$refText;

                if (!elementType) continue;


                const elementEntity = aggregate.entities?.find((e: any) => e.name === elementType);
                if (!elementEntity) continue;


                const isProjection = (elementEntity as any).aggregateRef !== undefined;


                let identifierField: string;
                if (isProjection) {

                    const referencedAggregateName = this.extractReferencedAggregateName(elementType);
                    identifierField = `${referencedAggregateName.toLowerCase()}AggregateId`;
                } else {

                    identifierField = this.determineBusinessKey(elementEntity);
                }


                const removedEventVariations = this.getEventNameVariations('Removed', elementType);
                collectionEvents.push({
                    eventType: 'Removed',
                    ...removedEventVariations,
                    properties: [
                        {
                            name: identifierField,
                            type: 'Integer',
                            capitalizedName: this.capitalize(identifierField),
                            isFinal: false,
                            isCollection: false,
                            isEntity: false
                        }
                    ],
                    isCollectionEvent: true,
                    elementType,
                    timestamp: new Date().toISOString()
                });



                if (!isProjection) {


                    const updatedEventVariations = this.getEventNameVariations('Updated', elementType);
                    collectionEvents.push({
                        eventType: 'Updated',
                        ...updatedEventVariations,
                        properties: [
                            {
                                name: identifierField,
                                type: 'Integer',
                                capitalizedName: this.capitalize(identifierField),
                                isFinal: false,
                                isCollection: false,
                                isEntity: false
                            }

                        ],
                        isCollectionEvent: true,
                        elementType,
                        timestamp: new Date().toISOString()
                    });
                }
            }
        }

        return collectionEvents;
    }



    private extractReferencedAggregateName(entityName: string): string {

        for (let i = entityName.length - 1; i >= 0; i--) {
            if (entityName[i] === entityName[i].toUpperCase() && i > 0) {
                const candidate = entityName.substring(i);
                if (candidate.length > 1) {
                    return candidate;
                }
            }
        }
        return entityName;
    }



    private determineBusinessKey(entity: any): string {
        if (!entity || !entity.properties) {
            return 'key';
        }


        const commonKeys = ['key', 'code', 'id', 'sequence'];

        for (const keyName of commonKeys) {
            const field = entity.properties.find((p: any) => p.name === keyName);
            if (field) {
                return keyName;
            }
        }


        const firstIntField = entity.properties.find((p: any) => {
            const javaType = this.resolveJavaType(p.type);
            return javaType === 'Integer' &&
                !p.name.endsWith('AggregateId') &&
                !p.name.endsWith('Version');
        });

        return firstIntField?.name || 'key';
    }

    private extractPrimitiveProjectionFields(entity: any, prefix: string): any[] {
        const fields: any[] = [];



        const fieldMappings = getEffectiveFieldMappings(entity as Entity);

        for (const mapping of fieldMappings) {
            const fieldName = mapping.entityField;


            if (fieldName === 'id' || fieldName.endsWith('AggregateId') ||
                fieldName.endsWith('Version') || fieldName.endsWith('State')) {
                continue;
            }


            const javaType = this.resolveJavaType(mapping.type);


            if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                this.isEntityType(mapping.type)) {
                continue;
            }


            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                fields.push({
                    name: fieldName,
                    type: javaType,
                    capitalizedName: this.capitalize(fieldName),
                    isFinal: false,
                    isCollection: false,
                    isEntity: false
                });
            }
        }


        for (const prop of entity.properties || []) {
            const propName = prop.name;


            if (propName === 'id' || propName.endsWith('AggregateId') ||
                propName.endsWith('Version') || propName.endsWith('State')) {
                continue;
            }


            const javaType = this.resolveJavaType(prop.type);


            if (javaType.startsWith('Set<') || javaType.startsWith('List<') ||
                this.isEntityType(prop.type)) {
                continue;
            }


            if (EXTENDED_PRIMITIVE_TYPES.some(t => javaType.includes(t))) {
                fields.push({
                    name: propName,
                    type: javaType,
                    capitalizedName: this.capitalize(propName),
                    isFinal: prop.isFinal || false,
                    isCollection: false,
                    isEntity: false
                });
            }
        }

        return fields;
    }

    private buildPublishedEventsImports(aggregate: Aggregate, options: EventGenerationOptions, publishedEvents: any[]): string[] {
        const baseImports = this.buildBaseImports(aggregate, options);

        return [
            ...baseImports,
            'import java.time.LocalDateTime;',
            'import java.io.Serializable;',
            'import com.fasterxml.jackson.annotation.JsonFormat;',
            ''
        ];
    }

    private async generateIndividualPublishedEvent(context: any): Promise<string> {
        const template = this.loadRawTemplate('events/published-event.hbs');
        const propertyImports: string[] = [];
        const addedImports = new Set<string>();

        if (context.event.properties && context.event.properties.length > 0) {
            context.event.properties.forEach((prop: any) => {
                if (prop.isEntity && prop.type) {
                    const entityPackage = this.generateEventPackageName(
                        context.basePackage || 'unknown',
                        context.projectName || 'unknown',
                        prop.referencedAggregateName || context.aggregateName || 'unknown',
                        'shared',
                        'dtos'
                    );
                    const importStmt = `import ${entityPackage}.${prop.type};`;
                    if (!addedImports.has(importStmt)) {
                        propertyImports.push(importStmt);
                        addedImports.add(importStmt);
                    }
                } else if (prop.isEnum && prop.enumType) {
                    const basePackage = context.basePackage || 'unknown';
                    const enumPackage = `${basePackage}.${(context.projectName || 'unknown').toLowerCase()}.shared.enums`;
                    const importStmt = `import ${enumPackage}.${prop.enumType};`;
                    if (!addedImports.has(importStmt)) {
                        propertyImports.push(importStmt);
                        addedImports.add(importStmt);
                    }
                } else if (prop.type) {

                    const type = prop.type;
                    if (type === 'LocalDateTime' || type.includes('LocalDateTime')) {
                        const importStmt = 'import java.time.LocalDateTime;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type === 'BigDecimal' || type.includes('BigDecimal')) {
                        const importStmt = 'import java.math.BigDecimal;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type.startsWith('List<')) {
                        const importStmt = 'import java.util.List;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    } else if (type.startsWith('Set<')) {
                        const importStmt = 'import java.util.Set;';
                        if (!addedImports.has(importStmt)) {
                            propertyImports.push(importStmt);
                            addedImports.add(importStmt);
                        }
                    }
                }
            });
        }

        const rawFields = context.event.properties || [];
        const eventPrefix = PublishedEventGenerator.camelToSnake(context.event.fullEventName);
        const decoratedFields = rawFields.map((f: any) => {
            if (!f || !f.name) return f;
            const fieldSnake = PublishedEventGenerator.camelToSnake(f.name);
            const fullName = `${eventPrefix}_${fieldSnake}`;
            let columnName = fullName.length <= 63
                ? fullName
                : PublishedEventGenerator.shortenIdentifier(fullName, 63);
            if (PublishedEventGenerator.isReservedColumnName(columnName)) {
                columnName = `\\"${columnName}\\"`;
            }
            return { ...f, columnAnnotation: `    @Column(name = "${columnName}")` };
        });
        const needsColumnImport = decoratedFields.some((f: any) => f.columnAnnotation);
        if (needsColumnImport) {
            const importStmt = 'import jakarta.persistence.Column;';
            if (!addedImports.has(importStmt)) {
                propertyImports.push(importStmt);
                addedImports.add(importStmt);
            }
        }

        const templateContext = {
            packageName: context.packageName,
            eventName: context.event.fullEventName,
            fields: decoratedFields,
            imports: propertyImports.length > 0 ? propertyImports.join('\n') : undefined
        };
        return this.renderTemplateFromString(template, templateContext);
    }

    private static readonly SQL_RESERVED_WORDS = new Set<string>([
        'all', 'analyse', 'analyze', 'and', 'any', 'array', 'as', 'asc', 'asymmetric',
        'authorization', 'between', 'both', 'case', 'cast', 'check', 'collate', 'column',
        'constraint', 'create', 'cross', 'current_catalog', 'current_date', 'current_role',
        'current_schema', 'current_time', 'current_timestamp', 'current_user', 'default',
        'deferrable', 'desc', 'distinct', 'do', 'else', 'end', 'except', 'false', 'fetch',
        'for', 'foreign', 'from', 'full', 'grant', 'group', 'having', 'in', 'initially',
        'inner', 'intersect', 'into', 'is', 'isnull', 'join', 'key', 'lateral', 'leading',
        'left', 'like', 'limit', 'localtime', 'localtimestamp', 'natural', 'not', 'notnull',
        'null', 'offset', 'on', 'only', 'or', 'order', 'outer', 'overlaps', 'placing',
        'primary', 'range', 'references', 'returning', 'right', 'rows', 'select', 'session_user',
        'set', 'similar', 'some', 'symmetric', 'system_user', 'table', 'then', 'to',
        'trailing', 'true', 'union', 'unique', 'user', 'using', 'value', 'values', 'verbose',
        'when', 'where', 'window', 'with', 'year'
    ]);

    static camelToSnake(name: string): string {
        return name.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
    }

    static shortenIdentifier(name: string, max: number): string {
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = ((hash << 5) - hash + name.charCodeAt(i)) | 0;
        }
        const suffix = `_${(hash >>> 0).toString(16).padStart(8, '0')}`;
        return name.slice(0, max - suffix.length) + suffix;
    }

    static isReservedColumnName(fieldName: string): boolean {
        const lower = fieldName.toLowerCase();
        if (PublishedEventGenerator.SQL_RESERVED_WORDS.has(lower)) return true;
        const snake = PublishedEventGenerator.camelToSnake(fieldName);
        return PublishedEventGenerator.SQL_RESERVED_WORDS.has(snake);
    }
}
