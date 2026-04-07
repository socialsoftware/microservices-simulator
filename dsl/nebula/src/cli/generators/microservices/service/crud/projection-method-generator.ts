import { Entity } from "../../../../../language/generated/ast.js";
import { EntityExt, AggregateExt, TypeGuards } from "../../../../types/ast-extensions.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getEvents, getEntities } from "../../../../utils/aggregate-helpers.js";
import { EventNameParser } from "../../../common/utils/event-name-parser.js";
import { EventHandlerCodeGenerator } from "./event-handler-code-generator.js";



export class ProjectionMethodGenerator {
    

    static generateProjectionMethods(aggregateName: string, aggregate: AggregateExt, projectName: string): string {
        const events = getEvents(aggregate);
        if (!events || !events.subscribedEvents) {
            return '';
        }

        
        const simpleSubscriptions = events.subscribedEvents.filter((sub: any) => {
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        if (simpleSubscriptions.length === 0) {
            return '';
        }

        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntity = aggregate.entities.find((e: Entity) => TypeGuards.isRootEntity(e as EntityExt));
        if (!rootEntity) {
            return '';
        }

        const methods: string[] = [];

        for (const subscription of simpleSubscriptions) {
            const eventTypeName = (subscription as any).eventType || '';
            if (!eventTypeName) continue;

            const isUpdate = eventTypeName.includes('Updated');
            const isDelete = eventTypeName.includes('Deleted');

            if (!isUpdate && !isDelete) {
                methods.push(
                    `    public void handle${eventTypeName}(Integer aggregateId, UnitOfWork unitOfWork) {\n` +
                    `    }`
                );
                continue;
            }

            
            let publisherAggregateName = EventNameParser.extractEntityName(eventTypeName);

            
            const entities = getEntities(aggregate);
            let projectionEntities = entities.filter((e: any) => {
                const aggregateRef = e.aggregateRef;
                return aggregateRef && aggregateRef.toLowerCase() === publisherAggregateName.toLowerCase();
            });

            
            
            if (projectionEntities.length === 0) {
                projectionEntities = entities.filter((e: any) => {
                    const aggregateRef = e.aggregateRef;
                    
                    return aggregateRef && eventTypeName.toLowerCase().includes(aggregateRef.toLowerCase());
                });

                
                if (projectionEntities.length > 0) {
                    const publisherAggregateNameFromRef = (projectionEntities[0] as any).aggregateRef;
                    if (publisherAggregateNameFromRef) {
                        
                        publisherAggregateName = publisherAggregateNameFromRef;
                    }
                }
            }

            
            

            
            if (isDelete) {
                methods.push(EventHandlerCodeGenerator.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'delete',
                    projectName
                ));
            } else if (isUpdate) {
                methods.push(EventHandlerCodeGenerator.generateEventHandlerMethod(
                    capitalizedAggregate,
                    lowerAggregate,
                    rootEntity,
                    projectionEntities,
                    publisherAggregateName,
                    eventTypeName,
                    'update',
                    projectName
                ));
            }
        }

        return methods.length > 0 ? '\n' + methods.join('\n\n') : '';
    }
}
