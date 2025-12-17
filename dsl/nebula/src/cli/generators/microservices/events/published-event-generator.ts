import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions, PublishedEventContext } from "./event-types.js";

export class PublishedEventGenerator extends EventBaseGenerator {
    async generatePublishedEvents(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};
        const context = this.buildPublishedEventsContext(aggregate, rootEntity, options);

        for (const event of context.publishedEvents) {
            const eventContext = {
                ...context,
                event
            };
            results[`published-event-${event.eventName}`] = await this.generateIndividualPublishedEvent(eventContext);
        }

        return results;
    }

    private buildPublishedEventsContext(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): PublishedEventContext {
        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const publishedEvents = this.buildPublishedEvents(rootEntity, baseContext.aggregateName);
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
                        } else if (t.$type === 'CollectionType' || t.$type === 'ListType' || t.$type === 'SetType') {
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
        const template = this.loadTemplate('events/published-event.hbs');
        const propertyImports: string[] = [];
        if (context.event.properties && context.event.properties.length > 0) {
            context.event.properties.forEach((prop: any) => {
                if (prop.isEntity && prop.type) {
                    const entityPackage = this.generatePackageName(
                        context.projectName || 'unknown',
                        prop.referencedAggregateName || context.aggregateName || 'unknown',
                        'shared',
                        'dtos'
                    );
                    propertyImports.push(`import ${entityPackage}.${prop.type};`);
                } else if (prop.isEnum && prop.enumType) {
                    const basePackage = this.getBasePackage();
                    const enumPackage = `${basePackage}.${(context.projectName || 'unknown').toLowerCase()}.shared.enums`;
                    propertyImports.push(`import ${enumPackage}.${prop.enumType};`);
                }
            });
        }

        const templateContext = {
            packageName: context.packageName,
            eventName: context.event.fullEventName,
            fields: context.event.properties || [],
            imports: propertyImports.length > 0 ? propertyImports.join('\n') : undefined
        };
        return this.renderTemplate(template, templateContext);
    }
}
