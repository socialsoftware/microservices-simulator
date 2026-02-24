import { Aggregate, Entity, References, ReferenceConstraint } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions } from "./event-types.js";



export class ReferencesGenerator extends EventBaseGenerator {

    async generateReferenceHandlers(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        const references = (aggregate as any).references as References | undefined;
        if (!references || !references.constraints || references.constraints.length === 0) {
            return results;
        }

        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const projectName = options.projectName.toLowerCase();
        const basePackage = this.getEventBasePackage(options);

        for (const constraint of references.constraints) {
            const refConstraint = constraint as ReferenceConstraint;
            const subscriptionCode = this.generateReferenceSubscription(
                aggregate,
                rootEntity,
                refConstraint,
                baseContext,
                projectName,
                basePackage
            );

            const handlerCode = this.generateReferenceHandler(
                aggregate,
                rootEntity,
                refConstraint,
                baseContext,
                projectName,
                basePackage
            );

            const targetAggregateName = refConstraint.targetAggregate;
            results[`ref-subscription-${targetAggregateName}`] = subscriptionCode;
            results[`ref-handler-${targetAggregateName}`] = handlerCode;
        }

        return results;
    }

    private generateReferenceSubscription(
        aggregate: Aggregate,
        rootEntity: Entity,
        constraint: ReferenceConstraint,
        baseContext: any,
        projectName: string,
        basePackage: string
    ): string {
        const aggregateName = aggregate.name;
        const targetAggregate = constraint.targetAggregate;
        const eventType = `${targetAggregate}DeletedEvent`;
        const className = `${aggregateName}Subscribes${targetAggregate}Deleted`;
        const lowerAggregate = aggregateName.toLowerCase();

        return `package ${basePackage}.${projectName}.microservices.${lowerAggregate}.events.subscribe;

import ${basePackage}.ms.domain.event.EventSubscription;
import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${aggregateName};
import ${basePackage}.${projectName}.events.${eventType};

public class ${className} extends EventSubscription {
    public ${className}(${aggregateName} ${lowerAggregate}) {
        super(${lowerAggregate}.getAggregateId(), 0, ${eventType}.class.getSimpleName());
    }
}
`;
    }

    private generateReferenceHandler(
        aggregate: Aggregate,
        rootEntity: Entity,
        constraint: ReferenceConstraint,
        baseContext: any,
        projectName: string,
        basePackage: string
    ): string {
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();
        const targetAggregate = constraint.targetAggregate;
        const eventType = `${targetAggregate}DeletedEvent`;
        const className = `${targetAggregate}DeletedEventHandler`;

        return `package ${basePackage}.${projectName}.microservices.${lowerAggregate}.events.handling.handlers;

import ${basePackage}.ms.domain.event.Event;
import ${basePackage}.${projectName}.microservices.${lowerAggregate}.aggregate.${aggregateName}Repository;
import ${basePackage}.${projectName}.microservices.${lowerAggregate}.coordination.eventProcessing.${aggregateName}EventProcessing;
import ${basePackage}.${projectName}.events.${eventType};

public class ${className} extends ${aggregateName}EventHandler {
    public ${className}(${aggregateName}Repository ${lowerAggregate}Repository, ${aggregateName}EventProcessing ${lowerAggregate}EventProcessing) {
        super(${lowerAggregate}Repository, ${lowerAggregate}EventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.${lowerAggregate}EventProcessing.process${targetAggregate}DeletedEvent(subscriberAggregateId, (${eventType}) event);
    }
}
`;
    }

}
